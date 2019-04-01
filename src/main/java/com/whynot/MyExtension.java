//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.whynot;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.tools.math.similarity.numerical.EuclideanDistance;
import java.util.Iterator;

public class MyExtension extends Operator {
    private EuclideanDistance distance_ = new EuclideanDistance();
    private InputPort in1 = (InputPort)this.getInputPorts().createPort("Input 1");
    private InputPort in2 = (InputPort)this.getInputPorts().createPort("Input 2");
    private OutputPort out = (OutputPort)this.getOutputPorts().createPort("Output");

    public MyExtension(OperatorDescription description) {
        super(description);
    }

    public void doWork() throws OperatorException {
        ExampleSet es = (ExampleSet)this.in1.getDataOrNull(ExampleSet.class);
        ExampleSet es2 = (ExampleSet)this.in2.getDataOrNull(ExampleSet.class);
        this.distance_.init(es);
        Iterator var3 = es.iterator();

        label40:
        while(var3.hasNext()) {
            Example e = (Example)var3.next();
            Example point1 = null;
            Example point2 = null;
            double minDist1 = 0.0D;
            double minDist2 = 0.0D;
            Iterator var11 = es2.iterator();

            while(true) {
                Example p;
                double disatnce;
                label35:
                do {
                    while(var11.hasNext()) {
                        p = (Example)var11.next();
                        disatnce = this.distance_.calculateDistance(e, p);
                        if (e.getLabel() == p.getLabel()) {
                            continue label35;
                        }

                        if (point2 == null || disatnce < minDist2) {
                            point2 = p;
                            minDist2 = disatnce;
                        }
                    }

                    e.put("Point_1", point1);
                    e.put("Point_2", point2);
                    continue label40;
                } while(point1 != null && disatnce >= minDist1);

                point1 = p;
                minDist1 = disatnce;
            }
        }

        this.out.deliver(es);
    }
}
