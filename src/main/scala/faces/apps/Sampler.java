package faces.apps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**Utility class provides uniform sampling in range [0, 1].
 <p>This sampling method follows this rule:</p>
 <ul>
 <li>If requested sample count is less than 1, the result is empty list [].</li>
 <li>If requested sample count is 1, the result is list contains single element [0.5].</li>
 <li>If requested sample count is 2, the result is list contains 2 elements [0.0, 1.0].</li>
 <li>If requested sample count is greater than 2, the result will be [0.0, (1.0 / (sampleCount - 1)), ..., 1.0].</li></ul>
 */
class Sampler {
    public static final List<Double> sampleByCount(int count) {
        if (count <= 0) return Collections.emptyList();
        if (count == 1) return Collections.singletonList(0.5);

        final List<Double> samples = new ArrayList<>(count);
        final double space = 1.0 / (count - 1);
        samples.add(0.0);
        for (int i = 1; i < count - 1; ++i)     samples.add(space * i);
        samples.add(1.0);

        return Collections.unmodifiableList(samples);
    }
}
