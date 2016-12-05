package netkit.util;

import java.util.logging.Logger;

public abstract class ComputeProcess {
	protected final Logger logger;

	private Object lock = new Object();

	private final String name;
	protected double progress = 0D;
	private boolean active = false;
	private boolean clean = true;
	
	protected ComputeProcess(final String name) {
		this.name = name;
		logger = NetKitEnv.getLogger("netkit.util."+name);
	}

	public double progress() { return progress; }
	public boolean active() { return active; }
	public void stop() { stop(false); }
	public void stop(final boolean wait) { 
		active=false;
		if(wait) {
			while(!clean) {
				try { 
					Thread.sleep(50);
				}
				catch(Exception ex)
				{

				}
			}
		}
	}

	public void start() {
		if(progress==1D)
			return;

		synchronized(lock) {
			if(active)
				throw new IllegalStateException(name()+" is already active!");
			active = true;
			clean = false;
		}
		logger.info(name()+": START Computing");

		final boolean success = run();
		synchronized(lock) {
			if(active) {
				active = false;
				if(success) {
					progress = 1D;
				} else {
					progress = 0D;
				}
			}
			clean = true;
		}
		
		logger.info(name()+": DONE Computing.  Finish="+success);

	}

	public String name() { return name; }

	/**
	 * This method is the main computation method.  It should check the active boolean
	 * regularly to see if it should abort the process.
	 * @return false if it aborted, true otherwise
	 */
	protected abstract boolean run();
}
