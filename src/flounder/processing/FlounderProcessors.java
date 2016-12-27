package flounder.processing;

import flounder.framework.*;
import flounder.logger.*;
import flounder.processing.opengl.*;
import flounder.processing.resource.*;
import flounder.profiling.*;

import java.util.*;

/**
 * A module used for processing types of requests.
 */
public class FlounderProcessors extends IModule {
	private static final FlounderProcessors instance = new FlounderProcessors();

	private List<IProcessor> processors;

	/**
	 * Creates a new request processor.
	 */
	public FlounderProcessors() {
		super(ModuleUpdate.UPDATE_PRE, FlounderLogger.class, FlounderProfiler.class);
	}

	@Override
	public void init() {
		this.processors = new ArrayList<>();

		processors.add(new ProcessorResource());
		processors.add(new ProcessorOpenGL());

		processors.forEach(processor -> {
			processor.init();
			processor.setInitialized(true);
		});
	}

	@Override
	public void update() {
		List<IExtension> newProcessors = FlounderModules.getExtensions(getInstance());

		if (newProcessors != null) {
			List<IProcessor> newCasted = new ArrayList<>();
			newProcessors.forEach(extension -> newCasted.add(((IProcessor) extension)));

			if (processors != null) {
				List<IProcessor> removedStandards = new ArrayList<>();
				removedStandards.addAll(processors);
				removedStandards.removeAll(newCasted);

				removedStandards.forEach(removed -> {
					removed.dispose();
					removed.setInitialized(false);
				});
			} else {
				processors = new ArrayList<>();
			}

			processors.clear();
			processors.addAll(newCasted);

			processors.forEach(standard -> {
				if (!standard.isInitialized()) {
					standard.init();
					standard.setInitialized(true);
				}
			});
		}

		if (processors != null && !processors.isEmpty()) {
			processors.forEach(IProcessor::update);
		}
	}

	@Override
	public void profile() {
		if (processors != null && !processors.isEmpty()) {
			processors.forEach(IProcessor::profile);
		}
	}

	/**
	 * Sends a new resource request to be added to a que.
	 *
	 * @param request The resource request to add.
	 */
	public static void sendRequest(Object request) {
		instance.processors.forEach(processor -> {
			if (processor.getRequestClass().isInstance(request)) {
				processor.addRequestToQueue(request);
			}
		});
	}

	@Override
	public IModule getInstance() {
		return instance;
	}

	@Override
	public void dispose() {
		if (processors != null && !processors.isEmpty()) {
			processors.forEach(processor -> {
				processor.dispose();
				processor.setInitialized(false);
			});
		}
	}
}
