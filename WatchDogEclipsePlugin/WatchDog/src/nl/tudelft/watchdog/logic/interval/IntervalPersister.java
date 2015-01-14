package nl.tudelft.watchdog.logic.interval;

import java.io.File;
import java.util.List;
import java.util.Set;

import nl.tudelft.watchdog.logic.interval.intervaltypes.IntervalBase;
import nl.tudelft.watchdog.util.WatchDogLogger;

import org.mapdb.DB;
import org.mapdb.DBMaker;

/**
 * Support for storing and querying intervals. The intervals are saved with a
 * long key, thus the maximum number of intervals that any single WatchDog
 * instance can record before the database breaks is {@link Long#MAX_VALUE}.
 */
public class IntervalPersister {

	private static final String INTERVALS = "intervals";

	private DB database;

	/** In memory representation of the interval store */
	private Set<IntervalBase> set;

	private File file;

	/**
	 * Create a new interval persister. If file points to an existing database
	 * of intervals, it will be reused.
	 */
	public IntervalPersister(final File file) {
		this.file = file;
		try {
			initalizeDatabase(file);
		} catch (Error e) {
			// MapDB wraps every exception inside an Error, so this code is
			// unfortunately necessary.
			try {
				recreateDatabase(file);
			} catch (Error innerError) {
				WatchDogLogger.getInstance().logSevere(innerError);
			}
		}
		try {
			// Compact database on every 10th new interval.
			if (!set.isEmpty() && set.size() % 10 == 0) {
				database.compact();
			}
		} catch (Error e) {
			// intentionally left empty.
		}
	}

	private void initalizeDatabase(File file) {
		try {
			database = createDatabase(file);
			set = createSet();
		} catch (RuntimeException e) {
			recreateDatabase(file);
		}
	}

	private Set<IntervalBase> createSet() {
		return database.getTreeSet(INTERVALS);
	}

	private DB createDatabase(final File file) {
		return DBMaker.newFileDB(file).closeOnJvmShutdown().make();
	}

	private void recreateDatabase(final File file) {
		closeDatabase();
		// Happens when an update to the serializables in the database
		// was made, and the new objects cannot be created from the old data
		deleteDatabaseFile();
		initalizeDatabase(file);
	}

	private void deleteDatabaseFile() {
		file.delete();
		File axuiliaryDatabaseFile = new File(file + ".p");
		axuiliaryDatabaseFile.delete();
		axuiliaryDatabaseFile = new File(file + ".t");
		axuiliaryDatabaseFile.delete();
	}

	/** Reads all intervals and returns them as a List. */
	public Set<IntervalBase> readIntervals() {
		return set;
	}

	/** Saves one interval to persistent storage */
	public void saveInterval(IntervalBase interval) {
		try {
			set.add(interval);
			// persist changes to disk
			database.commit();
		} catch (Error error) {
			try {
				recreateDatabase(file);
			} catch (Error innerError) {
				WatchDogLogger.getInstance().logSevere(innerError);
			}
		}

	}

	/** Removes the intervals from the database.. */
	public void removeIntervals(List<IntervalBase> intervalsToRemove) {
		for (IntervalBase interval : intervalsToRemove) {
			set.remove(interval);
		}
		database.commit();
	}

	/** @return The highest key in the database. -1, if the database is empty */
	public long getSize() {
		try {
			return set.size();
		} catch (Error | RuntimeException e) {
			return -1;
		}
	}

	/**
	 * Properly close the database. Note: The database should be ACID even when
	 * not properly closed.
	 */
	public void closeDatabase() {
		if (database != null && !database.isClosed()) {
			database.close();
		}
	}

	/** Clears the database on the computer and resets it. */
	public void clearAndResetMap() {
		if (database != null && !database.isClosed()) {
			database.delete(INTERVALS);
			database.commit();
			set = createSet();
		}
	}
}