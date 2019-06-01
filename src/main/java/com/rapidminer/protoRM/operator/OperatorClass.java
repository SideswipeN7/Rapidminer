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
import com.rapidminer.protoRM.PointContainer;
import com.rapidminer.protoRM.PointData;
import com.rapidminer.protoRM.PointPair;
import com.rapidminer.protoRM.PointType;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.container.Tupel;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasureHelper;
import com.rapidminer.tools.math.similarity.DistanceMeasures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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
//    private HashMap<Double, PointContainer> pointDataMap = new HashMap<>();
//    private HashMap<Double, Long> pairIdMap = new HashMap<>();
    private HashMap<Long, Integer> countersMap = new HashMap<>();
    private int maxSize_;
    private int minSize_;
    private double ratio_;
    private ExampleSet points_;
    private ExampleSet prototypes_;
    private DistanceMeasure distanceMeasure_;
    private HashMap<Long, LinkedList<PointContainer>> pairContainerListMap_ = new HashMap<>();
    private HashMap<Double, PointContainer> containerMap_ = new HashMap<>();

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


    public void doWork() throws OperatorException {
        //Get data
        getData();
        try {
            //Add attributes
            addAttributes();
            //Calculate Groups
            calculate();
            //Optimize results
            optimize();
            //Set attributes
            setAttributes();
        } catch (NullPointerException ex) {
            log(Level.SEVERE, ex.getMessage());
        }
        //Return data
        this.out.deliver(points_);
    }

    private void getData() throws OperatorException {
        log(Level.INFO, "Step 1: Get Data");
        points_ = this.in1.getDataOrNull(ExampleSet.class);
        prototypes_ = this.in2.getDataOrNull(ExampleSet.class);
        ratio_ = getParameterAsDouble(PARAMETER_RATIO);
        distanceMeasure_ = measureHelper.getInitializedMeasure(points_);
    }

    private void addAttributes() {
        log(Level.INFO, "Step 2: Add Attributes");
        //Add attributes to table
        points_.getExampleTable().addAttribute(a1);
        points_.getExampleTable().addAttribute(a2);
        points_.getExampleTable().addAttribute(a3);
        //Add attributes as Special
        points_.getAttributes().setSpecialAttribute(a1, "id_pair_1");
        points_.getAttributes().setSpecialAttribute(a2, "id_pair_2");
        points_.getAttributes().setSpecialAttribute(a3, "batch");
    }

    private void calculate() {
        log(Level.INFO, "Step 3: Calculate");
        //Get Attributes
        Attributes attributesExampleSet = points_.getAttributes();
        Attributes attributesPrototypes = prototypes_.getAttributes();
        for (Example point : points_) {
            double pointId = point.getId();
            log(Level.INFO, "########################");
            log(Level.INFO, "Point ID: " + pointId);
            log(Level.INFO, "########################");
            double[] valuesExample = getPointAttributes(attributesExampleSet, point);
            //New secondary point
            PointContainer pointContainer = new PointContainer();
            //Check distances
            for (Example prototype : prototypes_) {
                double prototypeId = prototype.getId();
                log(Level.INFO, "Prototype ID: " + prototypeId);
                //Calculate distanceMeasure
                double[] valuesPrototype = getPrototypeAttributes(attributesExampleSet, attributesPrototypes, prototype);
                double distance = distanceMeasure_.calculateDistance(valuesExample, valuesPrototype);
                PointData data = new PointData(prototypeId, distance);
                log(Level.INFO, "Distance: " + distance);
                //Set distances
                if (point.getLabel() == prototype.getLabel()) {
                    pointContainer.add(PointType.MyClass, data);
                } else {
                    pointContainer.add(PointType.OtherClass, data);
                }
            }
            log(Level.INFO, "Sorting");
            pointContainer.sort();
            //Set data
            addPointContainer(pointContainer);
            containerMap_.put(pointId, pointContainer);
        }
    }

    private void optimize() {
        log(Level.INFO, "Step 4: Optimize");
        calculateSizes();
        while (true) {
            int optimized = 0;
            int toOptimize = 0;
            for (long pairId : countersMap.keySet()) {
                if (countersMap.get(pairId) < minSize_) {
                    toOptimize++;
                    LinkedList<PointContainer> list = new LinkedList<>(pairContainerListMap_.get(pairId));
                    int countMaxed = 0;
                    for (PointContainer pc : list) {
                        if (pc.hasNext()) {
                            removePointContainer(pc, pairId);
                            pc.next();
                            addPointContainer(pc);
                            calculateSizes();
                        } else {
                            countMaxed++;
                        }
                    }
                    if (countMaxed == list.size()) {
                        optimized++;
                    }
                }
            }
            if (toOptimize == optimized) {
                break;
            }
        }
    }

    private void setAttributes() {
        log(Level.INFO, "Step 5: Set Attributes");
        for (Example point : points_) {
            double pointId = point.getId();
            PointContainer pc = containerMap_.get(pointId);
            PointPair pr = pc.get();
            point.setValue(a1, pr.getFirstPoint().getId());
            point.setValue(a2, pr.getSecondPoint().getId());
            point.setValue(a3, pr.getPairId());
        }
    }


    private void addPointContainer(PointContainer pointContainer) {
        long pairId = pointContainer.get().getPairId();
        log(Level.INFO, "Adding container for Pair ID: " + pairId);
        LinkedList<PointContainer> list = pairContainerListMap_.get(pairId);
        if (list == null) {
            list = new LinkedList<>();
            log(Level.WARNING, "No List for pair ID: " + pairId);
        }
        list.add(pointContainer);
        pairContainerListMap_.put(pairId, list);
        countersMap.put(pairId, list.size());
        log(Level.INFO, "Pair ID: " + pairId + " size: " + list.size());
    }

    private void removePointContainer(PointContainer pointContainer, long pairId) {
        LinkedList<PointContainer> list = pairContainerListMap_.get(pairId);
        list.remove(pointContainer);
        pairContainerListMap_.put(pairId, list);
        countersMap.put(pairId, list.size());
    }

    private void calculateSizes() {
        calculateMaxSize();
        calculateMinSize();
    }

    private void calculateMinSize() {
        minSize_ = (int) (ratio_ * maxSize_);
    }

    private void calculateMaxSize() {
        maxSize_ = -1;
        for (long key : countersMap.keySet()) {
            if (countersMap.get(key) > maxSize_) {
                maxSize_ = countersMap.get(key);
            }
        }
    }

    private void log(java.util.logging.Level level, String message) {
        if (getParameterAsBoolean(PARAMETER_DEBUG)) {
            logger.log(level, message);
        }
    }
}
