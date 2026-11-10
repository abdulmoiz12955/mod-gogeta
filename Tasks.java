package dev.gogeta.ascension;

import net.minecraft.server.level.ServerLevel;
import java.util.ArrayList;
import java.util.List;

/** Tiny server-tick scheduler for multi-tick attacks. */
public final class Tasks {
    public interface Task { /** @return true when finished */ boolean tick(ServerLevel level); }

    private record Entry(ServerLevel level, Task task) {}
    private static final List<Entry> ACTIVE = new ArrayList<>();

    public static void add(ServerLevel level, Task task) { ACTIVE.add(new Entry(level, task)); }

    public static void tick() {
        if (ACTIVE.isEmpty()) return;
        List<Entry> snapshot = new ArrayList<>(ACTIVE);
        List<Entry> done = new ArrayList<>();
        for (Entry e : snapshot) {
            boolean finished;
            try { finished = e.task.tick(e.level); }
            catch (Exception ex) { ex.printStackTrace(); finished = true; }
            if (finished) done.add(e);
        }
        ACTIVE.removeAll(done);
    }

    public static void clear() { ACTIVE.clear(); }

    private Tasks() {}
}
