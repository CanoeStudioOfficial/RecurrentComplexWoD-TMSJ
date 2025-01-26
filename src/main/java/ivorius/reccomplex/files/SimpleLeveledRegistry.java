/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.files;

import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.files.loading.LeveledRegistry;
import ivorius.reccomplex.utils.leveled.LeveledBiMap;
import ivorius.reccomplex.utils.RawResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lukas on 29.09.16.
 */
public class SimpleLeveledRegistry<S> implements LeveledRegistry<S>
{
    protected LeveledBiMap<String, S> items = new LeveledBiMap<>(LeveledRegistry.Level.values().length);
    protected LeveledBiMap<String, Status> stati = new LeveledBiMap<>(items.levels());

    protected boolean activeCacheValid = false;
    protected Map<String, S> activeMap = new HashMap<>();

    public String description;

    private Map<Class<? extends Module<SimpleLeveledRegistry<S>>>, Module<SimpleLeveledRegistry<S>>> modules = new HashMap<>();

    public SimpleLeveledRegistry(String description)
    {
        this.description = description;
    }

    public LeveledBiMap<String, S> contents()
    {
        return items;
    }

    public LeveledBiMap<String, Status> stati()
    {
        return stati;
    }

    public Map<String, S> map()
    {
        return Collections.unmodifiableMap(items.getMap());
    }

    public Map<String, S> map(ILevel level)
    {
        return Collections.unmodifiableMap(items.getMap(level.getLevel()));
    }

    public Map<String, S> activeMap()
    {
        ensureActiveCache();
        return Collections.unmodifiableMap(activeMap);
    }

    public Collection<S> all()
    {
        return Collections.unmodifiableCollection(items.getMap().values());
    }

    public Collection<S> allActive()
    {
        ensureActiveCache();
        return Collections.unmodifiableCollection(activeMap.values());
    }

    @Nullable
    public S getActive(String id)
    {
        ensureActiveCache();
        return activeMap.get(id);
    }

    @Override
    @Nullable
    public S get(String id)
    {
        return items.getMap().get(id);
    }

    @Override
    public Status status(String id)
    {
        return stati.getMap().get(id);
    }

    @Nonnull
    public Set<String> activeIDs()
    {
        ensureActiveCache();
        return Collections.unmodifiableSet(activeMap.keySet());
    }

    @Nonnull
    @Override
    public Set<String> ids()
    {
        return Collections.unmodifiableSet(items.getMap().keySet());
    }

    @Override
    public boolean has(String id)
    {
        return items.getMap().containsKey(id);
    }

    public boolean hasActive(String id)
    {
        ensureActiveCache();
        return activeMap.containsKey(id);
    }

    public String id(S s)
    {
        return items.getMap().inverse().get(s);
    }

    @Nullable
    public RawResourceLocation resourceLocation(S s)
    {
        String id = id(s);
        return id != null ? new RawResourceLocation(status(id).domain, id) : null;
    }

    @Override
    public S register(String id, String domain, S s, boolean active, ILevel level)
    {
        invalidateActiveCache();

        stati.put(id, new Status(id, active, domain, level), level.getLevel());
        S old = items.put(id, s, level.getLevel());

        RecurrentComplex.logger.trace(String.format(old != null ? "Replaced %s '%s' at level %s" : "Registered %s '%s' at level %s", description, id, level));

        invalidateCaches();

        return old;
    }

    @Override
    public S unregister(String id, ILevel level)
    {
        invalidateActiveCache();
        invalidateCaches();
        stati.remove(id, level.getLevel());
        return items.remove(id, level.getLevel());
    }

    @Override
    public void clear(ILevel level)
    {
        RecurrentComplex.logger.trace(String.format("Cleared all %s at level %s", description, level));
        invalidateActiveCache();
        items.clear(level.getLevel());
        stati.clear(level.getLevel());
    }

    private void ensureActiveCache()
    {
        if (!activeCacheValid)
        {
            activeMap = stati.getMap().values().stream()
                    .filter(LeveledRegistry.Status::isActive)
                    .map(LeveledRegistry.Status::getId).collect(Collectors.toMap(s -> s, s -> items.getMap().get(s)));
            activeCacheValid = true;
        }
    }

    private void invalidateActiveCache()
    {
        activeCacheValid = false;
    }

    public <T extends Module<SimpleLeveledRegistry<S>>> void registerModule(Class<T> type, T cache)
    {
        modules.put(type, cache);
        cache.setRegistry(this);
    }

    public <T extends Module> void registerModule(T cache)
    {
        //noinspection unchecked
        registerModule((Class<T>) cache.getClass(), cache);
    }

    public <T extends Module> T module(Class<T> cache)
    {
        //noinspection unchecked
        return (T) modules.get(cache);
    }

    protected void invalidateCaches()
    {
        modules.values().forEach(Module::invalidate);
    }

    public static abstract class Module<R extends SimpleLeveledRegistry>
    {
        protected R registry;

        public void setRegistry(R registry)
        {
            this.registry = registry;
        }

        public abstract void invalidate();
    }

    public class Status implements LeveledRegistry.Status
    {
        protected String id;
        protected boolean active;
        protected String domain;
        protected ILevel level;

        public Status(String id, boolean active, String domain, ILevel level)
        {
            this.id = id;
            this.active = active;
            this.domain = domain;
            this.level = level;
        }

        @Override
        public String getId()
        {
            return id;
        }

        @Override
        public boolean isActive()
        {
            return active;
        }

        @Override
        public void setActive(boolean active)
        {
            this.active = active;
            invalidateActiveCache();
        }

        @Override
        public String getDomain()
        {
            return domain;
        }

        @Override
        public ILevel getLevel()
        {
            return level;
        }
    }
}
