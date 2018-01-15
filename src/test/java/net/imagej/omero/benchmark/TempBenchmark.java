package net.imagej.omero.benchmark;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;

@Category(Benchmark.class)
public class TempBenchmark {

	@Rule
	public TestRule benchmarkRun = new BenchmarkRule();

	@Test
	@BenchmarkOptions(benchmarkRounds = 20, warmupRounds = 0)
	public void test() {
		int a = 9;
		int b = 30459;
		int c = a + b;
	}
}
