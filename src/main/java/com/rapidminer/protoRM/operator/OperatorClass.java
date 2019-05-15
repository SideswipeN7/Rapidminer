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
import com.rapidminer.protoRM.Cantor;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.math.similarity.numerical.EuclideanDistance;

import java.util.Objects;

public class OperatorClass extends Operator {

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
        //Get data
        ExampleSet points = this.in1.getDataOrNull(ExampleSet.class);
        ExampleSet vanillaPoints = this.in1.getDataOrNull(ExampleSet.class);
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
        //Create distance measurer
        EuclideanDistance distance = new EuclideanDistance();
        distance.init(vanillaPoints);
        //Main loop
        for (int i = 0; i < vanillaPoints.size(); ++i) {
            double minDist1 = Double.POSITIVE_INFINITY;
            double minDist2 = Double.POSITIVE_INFINITY;
            Example p1 = null;
            Example p2 = null;
            Example point = vanillaPoints.getExample(i);
            //Check distances
            for (Example prototype : prototypes) {
                //Calculate distance
                double currDistance = distance.calculateDistance(point, prototype);
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
                points.getExample(i).setValue(a1, Objects.requireNonNull(p1).getId());
                points.getExample(i).setValue(a2, Objects.requireNonNull(p2).getId());
                pairId = Cantor.pair((new Double(p1.getId())).longValue(), (new Double(p2.getId())).longValue());
            } else {
                points.getExample(i).setValue(a1, Objects.requireNonNull(p2).getId());
                points.getExample(i).setValue(a2, Objects.requireNonNull(p1).getId());
                pairId = Cantor.pair((new Double(p2.getId())).longValue(), (new Double(p1.getId())).longValue());
            }
            //Set new ID for pair of a1 and a2
            points.getExample(i).setValue(a3, pairId);
        }
        //Return data
        this.out.deliver(points);
    }
}
