package ca.ualberta.physics.cssdp.file.resource;

import static com.jayway.restassured.RestAssured.delete;
import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.junit.Assert;
import org.junit.Test;

import ca.ualberta.physics.cssdp.domain.file.Host;
import ca.ualberta.physics.cssdp.domain.file.Host.Protocol;
import ca.ualberta.physics.cssdp.file.FileServer;

import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

public class CacheResourceTest extends FileTestsScaffolding {

	@Test
	public void allBasicCacheOps() throws IOException {

		File internalCacheDir = new File(FileServer.properties().getString(
				"file.cache.root"));

		File cacheDir = new File(internalCacheDir,
				"6878/4d5d/4110/7460/df67/ba08/b728/7267/");

		int startCount = cacheDir.list().length;

		File file = new File("src/test/resources/cache_test_file.txt");
		System.out.println(file.getAbsolutePath());
		given().formParam("key", "addToCache")
				.and()
				.multiPart(file)
				.expect()
				.statusCode(201)
				.and()
				.header("location",
						"http://localhost:8080/file/cache.json/68784d5d41107460df67ba08b7287267")
				.when().put("/file/cache.json");

		int midCount = cacheDir.list().length;

		Response res = given()
				.formParam("key", "addDuplicateToCache")
				.and()
				.multiPart(file)
				.expect()
				.statusCode(201)
				.and()
				.header("location",
						"http://localhost:8080/file/cache.json/68784d5d41107460df67ba08b7287267")
				.when().put("/file/cache.json");

		int endCount = cacheDir.list().length;

		Assert.assertEquals(midCount, startCount + 1);
		Assert.assertEquals(midCount, endCount);

		String location = res.getHeader("location");
		final InputStream is = get(location).asInputStream();

		File tempDir = Files.createTempDir();
		File tempFile = new File(tempDir, "68784d5d41107460df67ba08b7287267");
		Files.copy(new InputSupplier<InputStream>() {

			@Override
			public InputStream getInput() throws IOException {
				return is;
			}
		}, tempFile);

		Assert.assertEquals(Files.toString(file, Charset.forName("UTF-8")),
				Files.toString(tempFile, Charset.forName("UTF-8")));

		tempFile.delete();
		tempDir.delete();

		expect().statusCode(200).when().delete(location);
		expect().statusCode(404).when().get(location);

		Assert.assertEquals(startCount, cacheDir.list().length);

	}

	@Test
	public void testGenerate409Error() {

		File file = new File("src/test/resources/cache_test_file.txt");
		System.out.println(file.getAbsolutePath());
		given().formParam("key", "addToCache")
				.and()
				.multiPart(file)
				.expect()
				.statusCode(201)
				.and()
				.header("location",
						"http://localhost:8080/file/cache.json/68784d5d41107460df67ba08b7287267")
				.when().put("/file/cache.json");

		file = new File("build.gradle");
		given().formParam("key", "addToCache").and().multiPart(file).expect()
				.statusCode(409).when().put("/file/cache.json");

	}

	@Test
	public void findMD5() {

		File file = new File("src/test/resources/cache_test_file.txt");
		System.out.println(file.getAbsolutePath());
		given().formParam("key", "addToCache")
				.and()
				.multiPart(file)
				.expect()
				.statusCode(201)
				.and()
				.header("location",
						"http://localhost:8080/file/cache.json/68784d5d41107460df67ba08b7287267")
				.when().put("/file/cache.json");

		String md5 = get("/file/cache.json/find?key=addToCache").asString();

		Assert.assertEquals("68784d5d41107460df67ba08b7287267", md5);
	}

	@Test
	public void addKey() {

		File file = new File("src/test/resources/cache_test_file.txt");
		System.out.println(file.getAbsolutePath());
		given().formParam("key", "addToCache")
				.and()
				.multiPart(file)
				.expect()
				.statusCode(201)
				.and()
				.header("location",
						"http://localhost:8080/file/cache.json/68784d5d41107460df67ba08b7287267")
				.when().put("/file/cache.json");

		given().formParam("key", "secondKey").expect().statusCode(200).when()
				.post("/file/cache.json/68784d5d41107460df67ba08b7287267/map");

		Assert.assertEquals("68784d5d41107460df67ba08b7287267",
				get("/file/cache.json/find?key=secondKey").asString());

	}

	@Test
	public void request() throws IOException {

		Host host = new Host("sunsite.ualberta.ca", "anonymous", "anonymous");
		host.setProtocol(Protocol.ftp);

		expect().statusCode(201)
				.given()
				.content(host)
				.and()
				.contentType(ContentType.JSON)
				.and()
				.header("cicstart_session",
						login(dataManager.getEmail(), "password"))
				.post("/file/host.json");

		String url = "ftp://sunsite.ualberta.ca/pub/Mirror/apache/commons/daemon/RELEASE-NOTES.txt";
		// String url = "ftp://ftp14.freebsd.org/pub/FreeBSD/README.TXT";
		String encodeUrl = URLEncoder.encode(url, "UTF-8");
		given().formParam("url", url)
				.expect()
				.statusCode(202)
				.and()
				.header("location",
						"http://localhost:8080/file/cache.json/find?key=" + url)
				.when().put("/file/cache.json/request");

		while (get("/file/cache.json/find?key=" + encodeUrl).statusCode() == 404) {
			try {
				Thread.sleep(2000);
				System.out.println("sleeping...");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		final String md5 = get("/file/cache.json/find?key=" + encodeUrl)
				.asString();

		File readme = new File("apache-commons-daemon-README.txt");
		Files.copy(new InputSupplier<InputStream>() {

			@Override
			public InputStream getInput() throws IOException {
				return get("/file/cache.json/" + md5).asInputStream();
			}

		}, readme);

		Assert.assertEquals(5685, readme.length());

		System.out.println(Files.toString(readme, Charset.defaultCharset()));

		delete("/file/cache.json/" + md5);
		readme.delete();

	}

}