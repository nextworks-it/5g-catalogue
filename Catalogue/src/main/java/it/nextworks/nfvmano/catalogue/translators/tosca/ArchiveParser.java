package it.nextworks.nfvmano.catalogue.translators.tosca;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.PostConstruct;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;

@Service
public class ArchiveParser {

	private static final Logger log = LoggerFactory.getLogger(ArchiveParser.class);

	private ByteArrayOutputStream metadata;
	private Map<String, ByteArrayOutputStream> templates = new HashMap<>();
	private ByteArrayOutputStream mainServiceTemplate;
	private List<String> folderNames = new ArrayList<>();
	private Set<String> admittedFolders = new HashSet<>();
	
	public ArchiveParser() {}
	
	@PostConstruct
	void init() {
		admittedFolders.add("Definitions/");
		admittedFolders.add("Files/");
		admittedFolders.add("Files/Tests/");
		admittedFolders.add("Files/Licences/");
		admittedFolders.add("Scripts/");
	}

	private void parseArchive(InputStream archive) throws IOException, MalformattedElementException {
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
					log.debug("Parsing Archive: found file with name " + fileName);

					if (fileName.toLowerCase().equalsIgnoreCase("tosca.meta")) {
						this.metadata = outStream;
					} else if (fileName.toLowerCase().endsWith(".yaml")) {
						this.templates.put(fileName, outStream);
					} else {
						// TODO: process remaining content
					}
				} else {
					log.debug("Parsing Archive: adding folder with name " + entry.getName() + " to folders list");
					folderNames.add(entry.getName());
				}
			}
		}
		if (this.metadata == null) {
			log.error("CSAR without TOSCA.meta");
			throw new MalformattedElementException("CSAR without TOSCA.meta");
		} else {
			try {
				setMainServiceTemplateFromMetadata(this.metadata.toByteArray());
			} catch (MalformattedElementException e) {
				log.error(e.getMessage());
			}
		}
		if (this.mainServiceTemplate == null) {
			log.error("CSAR without main service template");
			throw new MalformattedElementException("CSAR without main service template");
		}
		
		for (String fName : this.folderNames) {
			if (!this.admittedFolders.contains(fName)) {
				log.error("Folder with name " + fName + " not admitted in CSAR option#1 structure");
				throw new MalformattedElementException("Folder with name " + fName + " not admitted in CSAR option#1 structure");
			}
		}
	}

	private void setMainServiceTemplateFromMetadata(byte[] metadata) throws IOException, MalformattedElementException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(metadata), "UTF-8"));
		
		log.debug("Going to parse TOSCA.meta...");

		try {
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				} else {
					String regex = "^Entry-Definitions: (Definitions\\/[^\\\\]*\\.yaml)$";
					if (line.matches(regex)) {
						Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
						Matcher matcher = pattern.matcher(line);
						if (matcher.find()) {
							String mst_name = matcher.group(1);
							log.debug("Parsing metadata: found Main Service Template " + mst_name);
							if (this.templates.containsKey(mst_name)) {
								this.mainServiceTemplate = this.templates.get(mst_name);
							} else {
								log.error("Main Service Template specified in TOSCA.meta not present in CSAR Definitions directory");
								throw new MalformattedElementException(
										"Main Service Template specified in TOSCA.meta not present in CSAR Definitions directory");
							}
						}
					}
				}
			}
		} finally {
			reader.close();
		}
	}

	public DescriptorTemplate archiveToMainDescriptor(MultipartFile file)
			throws IOException, MalformattedElementException {

		DescriptorTemplate dt = null;

		if (!file.isEmpty()) {
			this.folderNames.clear();
			this.templates.clear();
			this.mainServiceTemplate = null;
			this.metadata = null;

			byte[] bytes = file.getBytes();

			InputStream input = new ByteArrayInputStream(bytes);
			
			log.debug("Going to parse archive " + file.getName() + "...");
			parseArchive(input);

			if (this.mainServiceTemplate != null) {
				log.debug("Going to parse main service template...");
				String mst_content = this.mainServiceTemplate.toString("UTF-8");
				dt = DescriptorsParser.stringToDescriptorTemplate(mst_content);
			}

			this.mainServiceTemplate.close();
			this.metadata.close();
			input.close();
		}

		return dt;
	}
}
