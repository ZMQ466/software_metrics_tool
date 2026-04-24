package com.metrics.modules;

import com.metrics.core.MetricsManager;
import com.metrics.model.ProjectMetricsResult;
import com.metrics.parser.EclipseJdtCodeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TraditionalMetricsCalculatorTest {
    @Test
    void sampleInput_javaDemo_metricsAreStable() {
        Path dir = Path.of("sample_input", "java_demo").toAbsolutePath();

        MetricsManager manager = new MetricsManager();
        manager.setParser(new EclipseJdtCodeParser());
        manager.registerCalculator(new TraditionalMetricsCalculator());

        ProjectMetricsResult result = manager.runAnalysis(dir.toString());

        assertEquals(117.0, result.getTotalLoc(), 0.0001);
        assertEquals(96.0, result.getTotalEffectiveLoc(), 0.0001);
        assertEquals(0.0, result.getTotalCommentLines(), 0.0001);
        assertEquals(21.0, result.getTotalBlankLines(), 0.0001);
        assertEquals(0.0, result.getCommentRate(), 0.0001);

        assertEquals(25.0 / 9.0, result.getAvgCyclomaticComplexity(), 0.0001);
        assertEquals(78.0 / 9.0, result.getAvgMethodLoc(), 0.0001);

        assertEquals(6, result.getMaxCyclomaticComplexity());
        assertEquals(1, result.getMaxMethodNestingDepth());
        assertEquals(0, result.getHighComplexityMethodCount());
    }

    @Test
    void tempProject_detectsCommentsNestingAndHighComplexity(@TempDir Path tempDir) throws Exception {
        String src = ""
                + "public class Deep {\n"
                + "    /* block\n"
                + "       comment */\n"
                + "    public void m(int x) {\n"
                + "        // line comment\n"
                + "        if (x > 0) {\n"
                + "            for (int i = 0; i < 10; i++) {\n"
                + "                while (x > i) {\n"
                + "                    x--; // inline\n"
                + "                }\n"
                + "            }\n"
                + "        } else if (x == 0) {\n"
                + "            x = 1;\n"
                + "        } else {\n"
                + "            x = 2;\n"
                + "        }\n"
                + "        int y = x > 0 ? 1 : 2;\n"
                + "        if (y > 0 && x > 0) {\n"
                + "            x++;\n"
                + "        }\n"
                + "    }\n"
                + "\n"
                + "    public int hc(int x) {\n"
                + "        int s = 0;\n"
                + "        if (x > 0) s++;\n"
                + "        if (x > 1) s++;\n"
                + "        if (x > 2) s++;\n"
                + "        if (x > 3) s++;\n"
                + "        if (x > 4) s++;\n"
                + "        if (x > 5) s++;\n"
                + "        if (x > 6) s++;\n"
                + "        if (x > 7) s++;\n"
                + "        if (x > 8) s++;\n"
                + "        if (x > 9) s++;\n"
                + "        return s;\n"
                + "    }\n"
                + "}\n";

        Path file = tempDir.resolve("Deep.java");
        Files.writeString(file, src);

        MetricsManager manager = new MetricsManager();
        manager.setParser(new EclipseJdtCodeParser());
        manager.registerCalculator(new TraditionalMetricsCalculator());

        ProjectMetricsResult result = manager.runAnalysis(tempDir.toString());

        assertTrue(result.getTotalLoc() > 0);
        assertTrue(result.getTotalCommentLines() > 0);
        assertTrue(result.getTotalBlankLines() > 0);
        assertTrue(result.getCommentRate() > 0);
        assertTrue(result.getTotalEffectiveLoc() < result.getTotalLoc());
        assertTrue(result.getMaxMethodNestingDepth() >= 3);
        assertEquals(1, result.getHighComplexityMethodCount());
        assertTrue(result.getMaxCyclomaticComplexity() >= 11);
    }
}
