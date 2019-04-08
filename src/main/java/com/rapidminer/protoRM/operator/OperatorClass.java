package com.rapidminer.protoRM.operator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.math.similarity.numerical.EuclideanDistance;

public class OperatorClass extends Operator {
    private EuclideanDistance distance_ = new EuclideanDistance();
    private InputPort in1 = this.getInputPorts().createPort("Input Points");
    private InputPort in2 = this.getInputPorts().createPort("Input Prototypes");
    private OutputPort out = this.getOutputPorts().createPort("Output");

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
    }

    public void doWork() throws OperatorException {
        ExampleSet points = this.in1.getDataOrNull(ExampleSet.class);
        ExampleSet prototypes = this.in2.getDataOrNull(ExampleSet.class);
        this.distance_.init(points);
        Attribute a1 = AttributeFactory.createAttribute("Point_1", Ontology.NUMERICAL);
        Attribute a2 = AttributeFactory.createAttribute("Point_2", Ontology.NUMERICAL);
        points = (ExampleSet)points.clone();
        points.getExampleTable().addAttribute(a1);
        points.getExampleTable().addAttribute(a2);
        points.getAttributes().addRegular(a1);
        points.getAttributes().addRegular(a2);
        for (Example point : points) {
            Example point1 = null;
            Example point2 = null;
            double minDist1 = Double.POSITIVE_INFINITY;
            double minDist2 = Double.POSITIVE_INFINITY;
            for (Example prototype : prototypes) {
                double distance = distance_.calculateDistance(point, prototype);
                if (point.getLabel() == prototype.getLabel()) {
                    if (point1 == null || distance < minDist1) {
                        point1 = prototype;
                        minDist1 = distance;
                    }
                } else if (point2 == null || distance < minDist2) {
                    point2 = prototype;
                    minDist2 = distance;
                }
            }

            point.setValue(a1, point1.getId());
            point.setValue(a2, point2.getId());
        }
        this.out.deliver(points);
    }
}
