package basyx.components.updater.examples.httppollingjsonataaas.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import basyx.components.updater.camelhttppolling.configuration.factory.HttpPollingDefaultConfigurationFactory;
import basyx.components.updater.cameltimer.configuration.factory.TimerDefaultConfigurationFactory;
import org.eclipse.basyx.aas.manager.ConnectedAssetAdministrationShellManager;
import org.eclipse.basyx.aas.metamodel.connected.ConnectedAssetAdministrationShell;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.eclipse.basyx.aas.registration.memory.InMemoryRegistry;
import org.eclipse.basyx.components.aas.AASServerComponent;
import org.eclipse.basyx.components.aas.configuration.AASServerBackend;
import org.eclipse.basyx.components.aas.configuration.BaSyxAASServerConfiguration;
import org.eclipse.basyx.components.configuration.BaSyxContextConfiguration;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.api.submodelelement.ISubmodelElement;
import org.junit.BeforeClass;
import org.junit.Test;

import basyx.components.updater.aas.configuration.factory.AASProducerDefaultConfigurationFactory;
import basyx.components.updater.core.component.UpdaterComponent;
import basyx.components.updater.core.configuration.factory.DefaultRoutesConfigurationFactory;
import basyx.components.updater.core.configuration.route.RoutesConfiguration;
import basyx.components.updater.transformer.cameljsonata.configuration.factory.JsonataDefaultConfigurationFactory;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;

public class TestAASUpdater {
	private static AASServerComponent aasServer;
	private static UpdaterComponent updater;
	private static InMemoryRegistry registry;

	protected static IIdentifier deviceAAS = new CustomId("TestUpdatedDeviceAAS");
	private static BaSyxContextConfiguration aasContextConfig;

	@BeforeClass
	public static void setUp() throws IOException {
		registry = new InMemoryRegistry();

		aasContextConfig = new BaSyxContextConfiguration(4001, "");
		BaSyxAASServerConfiguration aasConfig = new BaSyxAASServerConfiguration(AASServerBackend.INMEMORY, "aasx/updatertest.aasx");
		aasServer = new AASServerComponent(aasContextConfig, aasConfig);
		aasServer.setRegistry(registry);

		//Create and start MockServer
		ClientAndServer clientServer = ClientAndServer.startClientAndServer(2018);
		System.out.println("MockServer running: " + clientServer.isRunning());      //running status: true
		clientServer.when(new HttpRequest().withMethod("GET"))
				.respond(new HttpResponse().withStatusCode(HttpStatusCode.OK_200.code())
						.withBody("{\"objects\": \n" +
								"      [\n" +
								"        {\"name\":\"object1\", \"value\":858383},\n" +
								"        {\"name\":\"object2\", \"value\":42}\n" +
								"      ]\n" +
								"    }"));
	}

	@Test
	public void test() throws Exception {
		aasServer.startComponent();
		System.out.println("AAS STARTED");
		System.out.println("START UPDATER");
		ClassLoader loader = TestAASUpdater.class.getClassLoader();
		RoutesConfiguration configuration = new RoutesConfiguration();

		// Extend configutation for connections
		// DefaulRoutesConfigFac takes default routes.json as to config
		DefaultRoutesConfigurationFactory routesFactory = new DefaultRoutesConfigurationFactory(loader);
		configuration.addRoutes(routesFactory.getRouteConfigurations());

		// Extend configuration for Http Source
		HttpPollingDefaultConfigurationFactory httpPollingConfigFactory = new HttpPollingDefaultConfigurationFactory(loader);
		configuration.addDatasinks(httpPollingConfigFactory.getDataSinkConfigurations());

		// Extend configuration for AAS
		// DefaulRoutesConfigFactory takes default aasserver.json as to config
		AASProducerDefaultConfigurationFactory aasConfigFactory = new AASProducerDefaultConfigurationFactory(loader);
		configuration.addDatasinks(aasConfigFactory.getDataSinkConfigurations());

		// Extend configuration for Jsonata
		JsonataDefaultConfigurationFactory jsonataConfigFactory = new JsonataDefaultConfigurationFactory(loader);
		configuration.addTransformers(jsonataConfigFactory.getDataTransformerConfigurations());

		// Extend configuration for Timer
		TimerDefaultConfigurationFactory timerConfigFactory = new TimerDefaultConfigurationFactory(loader);
		configuration.addDatasources(timerConfigFactory.getDataSourceConfigurations());

		updater = new UpdaterComponent(configuration);
		updater.startComponent();
		System.out.println("UPDATER STARTED");
		checkIfPropertyIsUpdated();
		updater.stopComponent();
		aasServer.stopComponent();
	}

	private void checkIfPropertyIsUpdated() throws InterruptedException {
		ConnectedAssetAdministrationShellManager manager = new ConnectedAssetAdministrationShellManager(registry);
		ConnectedAssetAdministrationShell aas = manager.retrieveAAS(deviceAAS);
		ISubmodel sm = aas.getSubmodels().get("ConnectedSubmodel");

		ISubmodelElement updatedProp2 = sm.getSubmodelElement("ConnectedPropertyB");
		Object propValue2 = updatedProp2.getValue();
		System.out.println("UpdatedPROPB: " + propValue2);
		assertEquals("858383", propValue2);
	}
}
