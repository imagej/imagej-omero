
package net.imagej.omero.benchmark;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;

import java.io.IOException;

import net.imagej.omero.OMEROCredentials;
import net.imagej.omero.OMEROService;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;
import org.scijava.Context;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.ServerError;

@Category(Benchmark.class)
public class LowComplexityBenchmarkIT {

	private omero.client client;
	private OMEROCredentials cred;
	private Context context;
	private OMEROService omero;

	private static final String OMERO_SERVER = "localhost";
	private static final int OMERO_PORT = 4064;
	private static final String OMERO_USER = "root";
	private static final String OMERO_PASSWORD = "omero";

	@Rule
	public TestRule benchmarkRun = new BenchmarkRule();

	@Before
	public void setup() {
		client = new omero.client(OMERO_SERVER, OMERO_PORT);
		cred = new OMEROCredentials();
		cred.setServer(OMERO_SERVER);
		cred.setPort(OMERO_PORT);
		cred.setUser(OMERO_USER);
		cred.setPassword(OMERO_PASSWORD);

		context = new Context();
		omero = context.getService(OMEROService.class);
	}

	@After
	public void teardown() {
		context.dispose();
	}

	@Test
	@BenchmarkOptions(benchmarkRounds = 20, warmupRounds = 0)
	public void testDownloadImage() throws ServerError, IOException,
		CannotCreateSessionException, PermissionDeniedException
	{
		client.createSession(OMERO_USER, OMERO_PASSWORD);
		omero.downloadImage(client, 1);
		client.closeSession();
	}

}
