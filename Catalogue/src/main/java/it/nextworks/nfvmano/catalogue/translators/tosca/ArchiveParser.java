package it.nextworks.nfvmano.catalogue.translators.tosca;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import it.nextworks.nfvmano.libs.descriptors.common.templates.DescriptorTemplate;

@Service
public class ArchiveParser {

	@SuppressWarnings("resource")
	private void parseArchive(InputStream archive) throws IOException {
		ZipEntry entry;
		try (ZipInputStream zipStream = new ZipInputStream(archive)) {
			while ((entry = zipStream.getNextEntry()) != null) {

				if (!entry.isDirectory()) {
					ByteArrayOutputStream outStream = new ByteArrayOutputStream();

					int count;
					byte[] buffer = new byte[1024];
					while ((count = zipStream.read(buffer)) != -1) {
						outStream.write(buffer, 0, count);
					}

					String fileName = entry.getName();

					if (fileName.toLowerCase().equalsIgnoreCase("tosca.meta")) {
						// TODO: process metadata outStream
					} else if (fileName.toLowerCase().endsWith(".yaml")) {
						// TODO: process VNFD outStream
					} else {
						// TODO: process remaining content
					}
				}
			}
		}
	}

	public DescriptorTemplate archiveToDescriptors(MultipartFile file) throws IOException {
		
		if (!file.isEmpty()) {
			byte[] bytes = file.getBytes();
			
			InputStream input = new ByteArrayInputStream(bytes);
			parseArchive(input);
		}

		return null;
	}
}
