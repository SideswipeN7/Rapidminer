package com.rapidminer.protoRM.operator;

import com.rapidminer.example.Attribute;
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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

public class OperatorClass extends Operator implements CapabilityProvider {

    private InputPort in1 = this.getInputPorts().createPort("Input Points");
    private InputPort in2 = this.getInputPorts().createPort("Input Prototypes");
    private OutputPort out = this.getOutputPorts().createPort("Output");
    private DistanceMeasureHelper measureHelper = new DistanceMeasureHelper(this);

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
        Logger logger = Logger.getLogger(OperatorClass.class.getName());
        //Get data
        ExampleSet points = this.in1.getDataOrNull(ExampleSet.class);
        ExampleSet prototypes = this.in2.getDataOrNull(ExampleSet.class);
        //Create attributes
        Attribute a1 = AttributeFactory.createAttribute("ID_Proto_1", Ontology.NUMERICAL);
        Attribute a2 = AttributeFactory.createAttribute("ID_Proto_2", Ontology.NUMERICAL);
        Attribute a3 = AttributeFactory.createAttribute("ID_Proto_Pair", Ontology.NUMERICAL);
        //Add attributes to table
        points.getExampleTable().addAttribute(a1);
        points.getExampleTable().addAttribute(a2);
        points.getExampleTable().addAttribute(a3);
        //Add attributes as Special
        points.getAttributes().setSpecialAttribute(a1, "id_pair_1");
        points.getAttributes().setSpecialAttribute(a2, "id_pair_2");
        points.getAttributes().setSpecialAttribute(a3, "batch");
        DistanceMeasure distance = measureHelper.getInitializedMeasure(points);
        //Main loop
        for (Example point : points) {
            double minDist1 = Double.POSITIVE_INFINITY;
            double minDist2 = Double.POSITIVE_INFINITY;
            Example p1 = null;
            Example p2 = null;
            logger.log(Level.INFO, "########################");
            logger.log(Level.INFO, "Point ID: " + point.getId());
            logger.log(Level.INFO, "########################");
            //Check distances
            for (Example prototype : prototypes) {
                logger.log(Level.INFO, "Prototype ID: " + prototype.getId());
                //Calculate distance
                Example other = StreamSupport.stream(points.spliterator(), false).filter(d -> d.getId() == prototype.getId()).findFirst().get();
                double currDistance = distance.calculateDistance(point, other);
                logger.log(Level.INFO, "Distance: " + currDistance);
                //Set distances
                if (point.getLabel() == prototype.getLabel()) {
                    if (currDistance < minDist1) {
                        minDist1 = currDistance;
                        p1 = prototype;
                    }
                } else {
                    if (currDistance < minDist2) {
                        minDist2 = currDistance;
                        p2 = prototype;
                    }
                }
            }
            long pairId;
            //Set smallest ID as a1 and bigger ID as a2
            if (Objects.requireNonNull(p1).getId() < Objects.requireNonNull(p2).getId()) {
                point.setValue(a1, Objects.requireNonNull(p1).getId());
                point.setValue(a2, Objects.requireNonNull(p2).getId());
                pairId = Cantor.pair((new Double(p1.getId())).longValue(), (new Double(p2.getId())).longValue());
            } else {
                point.setValue(a1, Objects.requireNonNull(p2).getId());
                point.setValue(a2, Objects.requireNonNull(p1).getId());
                pairId = Cantor.pair((new Double(p2.getId())).longValue(), (new Double(p1.getId())).longValue());
            }
            //Set new ID for pair of a1 and a2
            point.setValue(a3, pairId);
        }
        //Return data
        this.out.deliver(points);
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
