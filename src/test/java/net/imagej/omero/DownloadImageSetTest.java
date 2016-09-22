
package net.imagej.omero;

import io.scif.FormatException;
import io.scif.img.ImgIOException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.imagej.ImgPlus;
import net.imagej.app.ImageJApp;

import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.plugin.Parameter;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import org.junit.Assert;
import omero.ServerError;
import omero.model.FilesetJobLinkI;

// @Ignore
// This requires a local OmeroServer
public class DownloadImageSetTest {

	private Context context;

	@Parameter
	private OMEROTransferService transfer;

	@Before
	public void setup() {
		if (context == null) {
			context = new Context();
			context.inject(this);
		}
	}

	@Test
	public void downloadImageSet() throws IOException {
		OMEROCredentials creds = setupCredentials();
		List<Long> ids = setupIds();

		Collection<ImgPlus<?>> res = transfer.downloadImageSet(creds, ids);
		Assert.assertEquals("Ammount of images downloaded not correct", res.size(),
			ids.size());

		ImgPlus<?> img = res.iterator().next();
	}

	private static List<Long> setupIds() {
		List<Long> ids = new ArrayList<>();
		ids.add(1l);
		ids.add(2l);
		ids.add(3l);
		return ids;
	}

	private static OMEROCredentials setupCredentials() {
		OMEROCredentials creds = new OMEROCredentials();
		creds.setServer("localhost");
		creds.setUser("root");
		creds.setPassword("password");
		creds.setEncrypted(true);
		creds.setPort(4064);
		return creds;
	}

}
