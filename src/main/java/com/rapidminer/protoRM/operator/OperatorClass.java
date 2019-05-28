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
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.protoRM.PointContainer;
import com.rapidminer.protoRM.PointData;
import com.rapidminer.protoRM.PointType;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.container.Tupel;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasureHelper;
import com.rapidminer.tools.math.similarity.DistanceMeasures;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OperatorClass extends Operator implements CapabilityProvider {
    private final String PARAMETER_DEBUG = "Debug";
    private final String PARAMETER_RATIO = "Ratio";

    private InputPort in1 = this.getInputPorts().createPort("Input Points");
    private InputPort in2 = this.getInputPorts().createPort("Input Prototypes");
    private OutputPort out = this.getOutputPorts().createPort("Output");
    private DistanceMeasureHelper measureHelper = new DistanceMeasureHelper(this);
    private Logger logger = Logger.getLogger(OperatorClass.class.getName());
    //Create attributes
    private Attribute a1 = AttributeFactory.createAttribute("ID_Proto_1", Ontology.NUMERICAL);
    private Attribute a2 = AttributeFactory.createAttribute("ID_Proto_2", Ontology.NUMERICAL);
    private Attribute a3 = AttributeFactory.createAttribute("ID_Proto_Pair", Ontology.NUMERICAL);
    //Optimize
    private HashMap<Double, PointContainer> pointDataMap = new HashMap<>();
    private HashMap<Double, Long> pairIdMap = new HashMap<>();
    private HashMap<Long, Integer> countersMap = new HashMap<>();
    private int biggestSize = -1;

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

    private void log(java.util.logging.Level level, String message) {
        if (getParameterAsBoolean(PARAMETER_DEBUG)) {
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
        types.add(new ParameterTypeDouble(PARAMETER_RATIO, "Ratio", 0, 1));
        types.add(new ParameterTypeBoolean(PARAMETER_DEBUG, "Logging on", false));
        return types;
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
        return valuesExample;
    }

    private void addAttributes(ExampleSet points) {
        //Add attributes to table
        points.getExampleTable().addAttribute(a1);
        points.getExampleTable().addAttribute(a2);
        points.getExampleTable().addAttribute(a3);
        //Add attributes as Special
        points.getAttributes().setSpecialAttribute(a1, "id_pair_1");
        points.getAttributes().setSpecialAttribute(a2, "id_pair_2");
        points.getAttributes().setSpecialAttribute(a3, "batch");
    }

    private void setPointAttributes(Example point, PointContainer pointData) {
        Tupel<PointData, PointData> tuple = pointData.getPair();
        long pairId = pointData.getPairId();
        pairIdMap.put(point.getId(), pairId);
        //Set point values
        point.setValue(a1, tuple.getFirst().getPointId());
        point.setValue(a2, tuple.getSecond().getPointId());
        point.setValue(a3, pairId);
        int counter;
        try {
            counter = countersMap.get(pairId);
        } catch (NullPointerException ex) {
            log(Level.WARNING, "No counter for pair ID: " + pairId);
            counter = 0;
        }
        counter++;
        if (counter > biggestSize) {
            biggestSize = counter;
        }
        countersMap.put(pairId, counter);
        pointDataMap.put(point.getId(), pointData);
    }

    public void doWork() throws OperatorException {
        biggestSize = 1;
        //Get data
        ExampleSet points = this.in1.getDataOrNull(ExampleSet.class);
        ExampleSet prototypes = this.in2.getDataOrNull(ExampleSet.class);
        addAttributes(points);
        //Main loop
        mainLoop(points, prototypes);
        //Optimize results
        optimize(points);
        //Return data
        this.out.deliver(points);
    }

    private void mainLoop(ExampleSet points, ExampleSet prototypes) throws OperatorException {
        DistanceMeasure distance = measureHelper.getInitializedMeasure(points);
        //Get Attributes
        Attributes attributesExampleSet = points.getAttributes();
        Attributes attributesPrototypes = prototypes.getAttributes();
        for (Example point : points) {
            log(Level.INFO, "########################");
            log(Level.INFO, "Point ID: " + point.getId());
            log(Level.INFO, "########################");
            double[] valuesExample = getPointAttributes(attributesExampleSet, point);
            //New secondary point
            PointContainer pointData = new PointContainer(point.getId());
            //Check distances
            for (Example prototype : prototypes) {
                log(Level.INFO, "Prototype ID: " + prototype.getId());
                //Calculate distance
                double[] valuesPrototype = getPrototypeAttributes(attributesExampleSet, attributesPrototypes, prototype);
                double currDistance = distance.calculateDistance(valuesExample, valuesPrototype);
                PointData calculated = new PointData(prototype.getId(), currDistance);
                log(Level.INFO, "Distance: " + currDistance);
                //Set distances
                if (point.getLabel() == prototype.getLabel()) {
                    pointData.addPoint(PointType.MyClass, calculated);
                } else {
                    pointData.addPoint(PointType.OtherClass, calculated);
                }
            }
            pointData.sort();
            setPointAttributes(point, pointData);
        }
    }

    private void optimize(ExampleSet points) {
        try {
            for (Example point : points) {
                while (true) {
                    int minSize = (int) (biggestSize * getParameterAsDouble(PARAMETER_RATIO));
                    log(Level.INFO, "Minimal size:" + minSize);
                    long pairId = pairIdMap.get(point.getId());
                    int counter = countersMap.get(pairId);
                    log(Level.INFO, "Point ID: " + point.getId() + " size:" + counter);
                    if (counter < minSize) {
                        try {
                            PointContainer data = pointDataMap.get(point.getId());
                            data.generateNewPair();
                            counter--;
                            countersMap.put(pairId, counter);
                            log(Level.INFO, "Optimizing point ID:" + point.getId());
                            setPointAttributes(point, data);
                            calculateBiggestCounter();
                        } catch (NullPointerException ex) {
                            log(Level.WARNING, "Cant optimize point ID:" + point.getId());
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
        } catch (UndefinedParameterError undefinedParameterError) {
            log(Level.WARNING, "No Ratio skipping optimize");
        }
    }

    private void calculateBiggestCounter() {
        biggestSize = -1;
        for (long key : countersMap.keySet()) {
            if (countersMap.get(key) > biggestSize) {
                biggestSize = countersMap.get(key);
            }
        }
    }
}
