package com.rapidminer.protoRM.operator;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.math.similarity.numerical.EuclideanDistance;

public class OperatorClass extends Operator {
    private EuclideanDistance distance_ = new EuclideanDistance();
    private InputPort in1 = this.getInputPorts().createPort("Input 1");
    private InputPort in2 = this.getInputPorts().createPort("Input 2");
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
        ExampleSet es = this.in1.getDataOrNull(ExampleSet.class);
        ExampleSet es2 = this.in2.getDataOrNull(ExampleSet.class);
        this.distance_.init(es);

        for (Example e : es) {
            Example point1 = null;
            Example point2 = null;
            double minDist1 = Double.POSITIVE_INFINITY;
            double minDist2 = Double.POSITIVE_INFINITY;
            for (Example e1 : es2) {
                double distance = this.distance_.calculateDistance(e, e1);
                if (e.getLabel() == e1.getLabel()) {
                    if (point1 == null || distance < minDist1) {
                        point1 = e1;
                        minDist1 = distance;
                    }
                } else if (point2 == null || distance < minDist2) {
                    point2 = e1;
                    minDist2 = distance;
                }
            }

            e.put("Point_1", point1);
            e.put("Point_2", point2);
        }
        this.out.deliver(es);
    }
}
