/*
 * Copyright 2004-2011 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.robot.db.allcommon;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.seasar.robot.dbflute.dbmeta.DBMeta;
import org.seasar.robot.dbflute.dbmeta.DBMetaProvider;
import org.seasar.robot.dbflute.exception.DBMetaNotFoundException;
import org.seasar.robot.dbflute.helper.StringKeyMap;
import org.seasar.robot.dbflute.util.DfAssertUtil;

/**
 * The handler of the instance of DB meta.
 * 
 * @author DBFlute(AutoGenerator)
 */
public class DBMetaInstanceHandler implements DBMetaProvider {

    // ===================================================================================
    // Resource Map
    // ============
    /** Table DB-name instance map. */
    protected static final Map<String, DBMeta> _tableDbNameInstanceMap =
        newConcurrentHashMap();

    /** The map of table DB name and class name. This is for initialization. */
    protected static final Map<String, String> _tableDbNameClassNameMap;
    static {
        final Map<String, String> tmpMap = newHashMap();
        tmpMap.put(
            "ACCESS_RESULT",
            "org.seasar.robot.db.bsentity.dbmeta.AccessResultDbm");
        tmpMap.put(
            "ACCESS_RESULT_DATA",
            "org.seasar.robot.db.bsentity.dbmeta.AccessResultDataDbm");
        tmpMap.put(
            "URL_FILTER",
            "org.seasar.robot.db.bsentity.dbmeta.UrlFilterDbm");
        tmpMap.put(
            "URL_QUEUE",
            "org.seasar.robot.db.bsentity.dbmeta.UrlQueueDbm");
        _tableDbNameClassNameMap = Collections.unmodifiableMap(tmpMap);
    }

    /** The flexible map of table DB name. This is for conversion at finding. */
    protected static final Map<String, String> _tableDbNameFlexibleMap =
        StringKeyMap.createAsFlexible();
    static {
        final Set<String> tableDbNameSet = _tableDbNameClassNameMap.keySet();
        for (final String tableDbName : tableDbNameSet) {
            _tableDbNameFlexibleMap.put(tableDbName, tableDbName);
        }
    }

    /**
     * Get the unmodifiable map of DB meta.
     * 
     * @return The unmodifiable map that contains all instances of DB meta.
     *         (NotNull & NotEmpty)
     */
    public static Map<String, DBMeta> getUnmodifiableDBMetaMap() {
        initializeDBMetaMap();
        synchronized (_tableDbNameInstanceMap) {
            return Collections.unmodifiableMap(_tableDbNameInstanceMap);
        }
    }

    /**
     * Initialize the map of DB meta.
     */
    protected static void initializeDBMetaMap() {
        if (isInitialized()) {
            return;
        }
        synchronized (_tableDbNameInstanceMap) {
            final Set<String> tableDbNameSet =
                _tableDbNameClassNameMap.keySet();
            for (final String tableDbName : tableDbNameSet) {
                findDBMeta(tableDbName); // Initialize!
            }
            if (!isInitialized()) {
                String msg = "Failed to initialize tableDbNameInstanceMap:";
                msg =
                    msg + " tableDbNameInstanceMap=" + _tableDbNameInstanceMap;
                throw new IllegalStateException(msg);
            }
        }
    }

    protected static boolean isInitialized() {
        return _tableDbNameInstanceMap.size() == _tableDbNameClassNameMap
            .size();
    }

    // ===================================================================================
    // Provider Singleton
    // ==================
    protected static final DBMetaProvider _provider =
        new DBMetaInstanceHandler();

    public static DBMetaProvider getProvider() {
        return _provider;
    }

    /**
     * @param tableFlexibleName
     *            The flexible name of table. (NotNull)
     * @return The instance of DB meta. (NullAllowed: If the DB meta is not
     *         found, it returns null)
     */
    public DBMeta provideDBMeta(final String tableFlexibleName) {
        return byTableFlexibleName(tableFlexibleName);
    }

    /**
     * @param tableFlexibleName
     *            The flexible name of table. (NotNull)
     * @return The instance of DB meta. (NotNull)
     * @exception org.seasar.robot.dbflute.exception.DBMetaNotFoundException
     *                When the DB meta is not found.
     */
    public DBMeta provideDBMetaChecked(final String tableFlexibleName) {
        return findDBMeta(tableFlexibleName);
    }

    // ===================================================================================
    // Find DBMeta
    // ===========
    /**
     * Find DB meta by table flexible name. (accept quoted name and schema
     * prefix)
     * 
     * @param tableFlexibleName
     *            The flexible name of table. (NotNull)
     * @return The instance of DB meta. (NotNull)
     * @exception org.seasar.robot.dbflute.exception.DBMetaNotFoundException
     *                When the DB meta is not found.
     */
    public static DBMeta findDBMeta(final String tableFlexibleName) {
        final DBMeta dbmeta = byTableFlexibleName(tableFlexibleName);
        if (dbmeta == null) {
            String msg =
                "The DB meta was not found by the table flexible name: "
                    + tableFlexibleName;
            msg =
                msg + " key=" + tableFlexibleName + " instanceMap="
                    + _tableDbNameInstanceMap;
            throw new DBMetaNotFoundException(msg);
        }
        return dbmeta;
    }

    // ===================================================================================
    // By Table Name
    // =============
    /**
     * @param tableFlexibleName
     *            The flexible name of table. (NotNull)
     * @return The instance of DB meta. (NullAllowed: If the DB meta is not
     *         found, it returns null)
     */
    protected static DBMeta byTableFlexibleName(String tableFlexibleName) {
        assertStringNotNullAndNotTrimmedEmpty(
            "tableFlexibleName",
            tableFlexibleName);
        tableFlexibleName = removeSchemaIfExists(tableFlexibleName);
        tableFlexibleName = removeQuoteIfExists(tableFlexibleName);
        final String tableDbName =
            _tableDbNameFlexibleMap.get(tableFlexibleName);
        if (tableDbName != null) {
            return byTableDbName(tableDbName);
        }
        return null;
    }

    protected static String removeSchemaIfExists(String name) {
        final int dotLastIndex = name.lastIndexOf(".");
        if (dotLastIndex >= 0) {
            name = name.substring(dotLastIndex + ".".length());
        }
        return name;
    }

    protected static String removeQuoteIfExists(String name) {
        if (name.startsWith("\"") && name.endsWith("\"")) {
            name = name.substring(1);
            name = name.substring(0, name.length() - 1);
        } else if (name.startsWith("[") && name.endsWith("]")) {
            name = name.substring(1);
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }

    /**
     * @param tableDbName
     *            The DB name of table. (NotNull)
     * @return The instance of DB meta. (NullAllowed: If the DB meta is not
     *         found, it returns null)
     */
    protected static DBMeta byTableDbName(final String tableDbName) {
        assertStringNotNullAndNotTrimmedEmpty("tableDbName", tableDbName);
        return getCachedDBMeta(tableDbName);
    }

    // ===================================================================================
    // Cached DBMeta
    // =============
    protected static DBMeta getCachedDBMeta(final String tableDbName) { // lazy-load
        // (thank you
        // koyak!)
        DBMeta dbmeta = _tableDbNameInstanceMap.get(tableDbName);
        if (dbmeta != null) {
            return dbmeta;
        }
        synchronized (_tableDbNameInstanceMap) {
            dbmeta = _tableDbNameInstanceMap.get(tableDbName);
            if (dbmeta != null) {
                return dbmeta;
            }
            final String entityName = _tableDbNameClassNameMap.get(tableDbName);
            _tableDbNameInstanceMap.put(tableDbName, getDBMeta(entityName));
            return _tableDbNameInstanceMap.get(tableDbName);
        }
    }

    protected static DBMeta getDBMeta(final String className) {
        try {
            final Class<?> clazz = Class.forName(className);
            final Method methoz =
                clazz.getMethod("getInstance", (Class[]) null);
            final Object result = methoz.invoke(null, (Object[]) null);
            return (DBMeta) result;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===================================================================================
    // General Helper
    // ==============
    protected static <KEY, VALUE> HashMap<KEY, VALUE> newHashMap() {
        return new HashMap<KEY, VALUE>();
    }

    protected static <KEY, VALUE> ConcurrentHashMap<KEY, VALUE> newConcurrentHashMap() {
        return new ConcurrentHashMap<KEY, VALUE>();
    }

    // -----------------------------------------------------
    // Assert Object
    // -------------
    protected static void assertObjectNotNull(final String variableName,
            final Object value) {
        DfAssertUtil.assertObjectNotNull(variableName, value);
    }

    // -----------------------------------------------------
    // Assert String
    // -------------
    protected static void assertStringNotNullAndNotTrimmedEmpty(
            final String variableName, final String value) {
        DfAssertUtil.assertStringNotNullAndNotTrimmedEmpty(variableName, value);
    }
}
