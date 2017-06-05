package am.ik.blog.entry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

public class EntryFactory {
	final Yaml yaml;
	final FrontMatterFactory frontMatterFactory;

	public EntryFactory() {
		this(new Yaml());
	}

	public EntryFactory(Yaml yaml) {
		this.yaml = yaml;
		this.frontMatterFactory = new FrontMatterFactory(yaml);
	}

	static boolean isPublic(Resource file) {
		return file != null && Entry.isPublicFileName(file.getFilename());
	}

	static EntryId parseEntryId(Resource file) {
		return EntryId.fromFileName(file.getFilename());
	}

	public Optional<Entry.EntryBuilder> parseBody(EntryId entryId, InputStream body) {
		final BufferedReader reader = new BufferedReader(
				new InputStreamReader(body, StandardCharsets.UTF_8));
		Entry.EntryBuilder entryBuilder = Entry.builder();
		entryBuilder.entryId(entryId);
		try {
			{
				final StringBuilder out = new StringBuilder();
				final String firstLine = reader.readLine();
				if (FrontMatter.SEPARATOR.equals(firstLine)) {
					for (String line = reader.readLine(); line != null
							&& !FrontMatter.SEPARATOR.equals(line); line = reader
									.readLine()) {
						out.append(line);
						out.append(System.lineSeparator());
					}
					String yaml = out.toString();
					FrontMatter frontMatter = frontMatterFactory.createFromYaml(yaml);
					entryBuilder.frontMatter(frontMatter);
				}
				else {
					return Optional.empty();
				}
			}
			{
				final StringBuilder out = new StringBuilder();
				for (String line = reader.readLine(); line != null; line = reader
						.readLine()) {
					out.append(line);
					out.append(System.lineSeparator());
				}
				String content = out.toString().trim();
				entryBuilder.content(new Content(Emoji.parse(content)));
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return Optional.of(entryBuilder);
	}

	public Optional<Entry.EntryBuilder> createFromYamlFile(Resource file) {
		if (!isPublic(file)) {
			return Optional.empty();
		}
		EntryId entryId = parseEntryId(file);
		try (InputStream stream = file.getInputStream()) {
			return parseBody(entryId, stream);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
