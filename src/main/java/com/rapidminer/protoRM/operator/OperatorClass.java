package com.rapidminer.protoRM.operator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.CapabilityPrecondition;
import com.rapidminer.operator.ports.metadata.DistanceMeasurePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.protoRM.Cantor;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasureHelper;
import com.rapidminer.tools.math.similarity.DistanceMeasures;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OperatorClass extends Operator implements CapabilityProvider {

    private InputPort in1 = this.getInputPorts().createPort("Input Points");
    private InputPort in2 = this.getInputPorts().createPort("Input Prototypes");
    private OutputPort out = this.getOutputPorts().createPort("Output");
    private DistanceMeasureHelper measureHelper = new DistanceMeasureHelper(this);
    private Logger logger = Logger.getLogger(OperatorClass.class.getName());
    private boolean isDebug = false;
    //Create attributes
    private Attribute a1 = AttributeFactory.createAttribute("ID_Proto_1", Ontology.NUMERICAL);
    private Attribute a2 = AttributeFactory.createAttribute("ID_Proto_2", Ontology.NUMERICAL);
    private Attribute a3 = AttributeFactory.createAttribute("ID_Proto_Pair", Ontology.NUMERICAL);

    /**
     * <p>
     * Creates an unnamed operator. Subclasses must pass the given description object to this
     * super-constructor (i.e. invoking super(OperatorDescription)). They might also add additional
     * values for process logging.
     * </p>
     * <p>
     * NOTE: the preferred way for operator creation is using one of the factory methods of
     * {@link OperatorService}.
     * </p>
     *
     * @param description
     */
    public OperatorClass(OperatorDescription description) {
        super(description);
        in1.addPrecondition(new DistanceMeasurePrecondition(in1, this));
        in1.addPrecondition(new CapabilityPrecondition(capability -> {
            int measureType = DistanceMeasures.MIXED_MEASURES_TYPE;
            try {
                measureType = measureHelper.getSelectedMeasureType();
            } catch (Exception ignored) {
            }
            switch (capability) {
                case BINOMINAL_ATTRIBUTES:
                case POLYNOMINAL_ATTRIBUTES:
                    return (measureType == DistanceMeasures.MIXED_MEASURES_TYPE)
                            || (measureType == DistanceMeasures.NOMINAL_MEASURES_TYPE);
                case NUMERICAL_ATTRIBUTES:
                    return (measureType == DistanceMeasures.MIXED_MEASURES_TYPE)
                            || (measureType == DistanceMeasures.DIVERGENCES_TYPE)
                            || (measureType == DistanceMeasures.NUMERICAL_MEASURES_TYPE);
                case MISSING_VALUES:
                    return true;
                default:
                    return false;
            }
        }, in1));
    }

    public void doWork() throws OperatorException {
        //Set Logger

        //Get data
        ExampleSet points = this.in1.getDataOrNull(ExampleSet.class);
        ExampleSet prototypes = this.in2.getDataOrNull(ExampleSet.class);

        //Add attributes to table
        points.getExampleTable().addAttribute(a1);
        points.getExampleTable().addAttribute(a2);
        points.getExampleTable().addAttribute(a3);
        //Add attributes as Special
        points.getAttributes().setSpecialAttribute(a1, "id_pair_1");
        points.getAttributes().setSpecialAttribute(a2, "id_pair_2");
        points.getAttributes().setSpecialAttribute(a3, "batch");
        DistanceMeasure distance = measureHelper.getInitializedMeasure(points);
        //Get Attributes
        Attributes attributesExampleSet = points.getAttributes();
        Attributes attributesPrototypes = prototypes.getAttributes();
        //Main loop
        for (Example point : points) {
            double minDist1 = Double.POSITIVE_INFINITY;
            double minDist2 = Double.POSITIVE_INFINITY;
            double p1 = -1;
            double p2 = -1;
            log(Level.INFO, "########################");
            log(Level.INFO, "Point ID: " + point.getId());
            log(Level.INFO, "########################");
            double[] valuesExample = getPointAttributes(attributesExampleSet, point);
            //Check distances
            for (Example prototype : prototypes) {
                log(Level.INFO, "Prototype ID: " + prototype.getId());
                //Calculate distance
                double[] valuesPrototype = getPrototypeAttributes(attributesExampleSet, attributesPrototypes, prototype);
                double currDistance = distance.calculateDistance(valuesExample, valuesPrototype);
                log(Level.INFO, "Distance: " + currDistance);
                //Set distances
                if (point.getLabel() == prototype.getLabel()) {
                    if (currDistance < minDist1) {
                        minDist1 = currDistance;
                        p1 = prototype.getId();
                    }
                } else {
                    if (currDistance < minDist2) {
                        minDist2 = currDistance;
                        p2 = prototype.getId();
                    }
                }
            }
            SetPointAttributes(point, p1, p2);
        }
        //Return data
        this.out.deliver(points);
    }

    private void SetPointAttributes(Example point, double p1, double p2) {
        //Set smallest ID as a1 and bigger ID as a2
        if (p1 > p2) {
            double temp = p2;
            p2 = p1;
            p1 = temp;
        }
        long pairId = Cantor.pair((long)p1, (long)p2);
        //Set point values
        point.setValue(a1, p1);
        point.setValue(a2, p2);
        point.setValue(a3, pairId);
    }

    private double[] getPrototypeAttributes(Attributes attributesPoints, Attributes attributesPrototypes, Example prototype) {
        int numAttrs = attributesPoints.size();
        double[] valuesPrototype = new double[numAttrs];
        int i = 0;
        for (Attribute attrName : attributesPoints) {
            Attribute prototypeAttribute = attributesPrototypes.get(attrName.getName());
            valuesPrototype[i++] = prototype.getValue(prototypeAttribute);
        }
        return valuesPrototype;
    }

    private double[] getPointAttributes(Attributes attributes, Example point) {
        int numAttrs = attributes.size();
        double[] valuesExample = new double[numAttrs];
        int i = 0;
        for (Attribute attrName : attributes) {
            valuesExample[i++] = point.getValue(attrName);
        }
        return  valuesExample;
    }


    private void log(java.util.logging.Level level, String message) {
        if (isDebug) {
            logger.log(level, message);
        }
    }


    @Override
    public boolean supportsCapability(OperatorCapability capability) {
        int measureType = DistanceMeasures.MIXED_MEASURES_TYPE;
        try {
            measureType = measureHelper.getSelectedMeasureType();
        } catch (Exception e) {
        }
        switch (capability) {
            case BINOMINAL_ATTRIBUTES:
            case POLYNOMINAL_ATTRIBUTES:
                return (measureType == DistanceMeasures.MIXED_MEASURES_TYPE)
                        || (measureType == DistanceMeasures.NOMINAL_MEASURES_TYPE);
            case NUMERICAL_ATTRIBUTES:
                return (measureType == DistanceMeasures.MIXED_MEASURES_TYPE)
                        || (measureType == DistanceMeasures.DIVERGENCES_TYPE)
                        || (measureType == DistanceMeasures.NUMERICAL_MEASURES_TYPE);
            case POLYNOMINAL_LABEL:
            case BINOMINAL_LABEL:
            case NUMERICAL_LABEL:
            case MISSING_VALUES:
                return true;
            default:
                return false;
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();

        types.addAll(DistanceMeasures.getParameterTypes(this));
        return types;
    }

}
