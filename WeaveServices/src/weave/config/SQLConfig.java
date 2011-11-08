/*
 * Weave (Web-based Analysis and Visualization Environment) Copyright (C) 2008-2011 University of Massachusetts Lowell This file is a part of Weave.
 * Weave is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License, Version 3, as published by the
 * Free Software Foundation. Weave is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the
 * GNU General Public License along with Weave. If not, see <http://www.gnu.org/licenses/>.
 */

package weave.config;

import java.rmi.RemoteException;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.w3c.dom.Document;

import weave.utils.SQLResult;
import weave.utils.SQLUtils;

/**
 * DatabaseConfig This class reads from an SQL database and provides an interface to retrieve strings.
 * 
 * @author Philip Kovac
 * @author Andy Dufilie
 */
public class SQLConfig
		implements ISQLConfig
{
//	private final String SQLTYPE_VARCHAR = "VARCHAR(256)";
//	private final String SQLTYPE_LONG_VARCHAR = "VARCHAR(2048)";
//	private final String SQLTYPE_INT = "INT";
	
	private final String SUFFIX_DESC = "attr_desc";
	private final String SUFFIX_META_PRIVATE = "attr_meta_private";
	private final String SUFFIX_META_PUBLIC = "attr_meta_public";
	private final String SUFFIX_HIERARCHY = "hierarchy";
	private final String WEAVE_TABLE_PREFIX = "weave_";
	
	private final String ID = "id";
	private final String DESCRIPTION = "description";
	private final String PROPERTY = "property";
	private final String VALUE = "value";
	
	private String table_desc = WEAVE_TABLE_PREFIX + SUFFIX_DESC;
	private String table_meta_private = WEAVE_TABLE_PREFIX + SUFFIX_META_PRIVATE;
	private String table_meta_public = WEAVE_TABLE_PREFIX + SUFFIX_META_PUBLIC;
	private String table_hierarchy = WEAVE_TABLE_PREFIX + SUFFIX_HIERARCHY;
	
	private DatabaseConfigInfo dbInfo = null;
	private ISQLConfig connectionConfig = null;
	private Connection _lastConnection = null; // do not use this variable directly -- use getConnection() instead.

	/**
	 * This function gets a connection to the database containing the configuration information. This function will reuse a previously created
	 * Connection if it is still valid.
	 * 
	 * @return A Connection to the SQL database.
	 */
	public Connection getConnection() throws RemoteException, SQLException
	{
		if (SQLUtils.connectionIsValid(_lastConnection))
			return _lastConnection;
		return _lastConnection = SQLConfigUtils.getConnection(connectionConfig, dbInfo.connection);
	}

	/**
	 * @param connectionConfig An ISQLConfig instance that contains connection information. This is required because the connection information is not stored in the database.
	 */
	public SQLConfig(ISQLConfig connectionConfig)
			throws RemoteException, SQLException, InvalidParameterException
	{
		// save original db config info
		dbInfo = connectionConfig.getDatabaseConfigInfo();
		if (dbInfo == null || dbInfo.schema == null || dbInfo.schema.length() == 0)
			throw new InvalidParameterException("DatabaseConfig: Schema not specified.");

		this.connectionConfig = connectionConfig;
		if (getConnection() == null)
			throw new InvalidParameterException("DatabaseConfig: Unable to connect to connection \"" + dbInfo.connection + "\"");

		// attempt to create the schema and tables to store the configuration.
		try
		{
			SQLUtils.createSchema(getConnection(), dbInfo.schema);
		}
		catch (Exception e)
		{
			// do nothing if schema creation fails -- temporary workaround for postgresql issue
			// e.printStackTrace();
		}
		initSQLTables();
	}
	private void initSQLTables() throws RemoteException, SQLException
	{
		Connection conn = getConnection();
		
		// ID->Description table
		List<String> columnNames = Arrays.asList(ID, DESCRIPTION);
		List<String> columnTypes = Arrays.asList(SQLUtils.getSerialPrimaryKeyTypeString(conn), "TEXT");
		SQLUtils.createTable(conn, dbInfo.schema, table_desc, columnNames, columnTypes);
		
		// Metadata tables
		columnNames = Arrays.asList(ID, PROPERTY, VALUE);
		columnTypes = Arrays.asList("BIGINT UNSIGNED", "TEXT", "TEXT");
		SQLUtils.createTable(conn, dbInfo.schema, table_meta_private, columnNames, columnTypes);
		SQLUtils.createTable(conn, dbInfo.schema, table_meta_public, columnNames, columnTypes);
		
		SQLUtils.addForeignKey(conn, dbInfo.schema, table_meta_private, ID, table_desc, ID);
		SQLUtils.addForeignKey(conn, dbInfo.schema, table_meta_public, ID, table_desc, ID);
		
		// Category table
		columnNames = Arrays.asList(ID, "name", "parent_id");
		columnTypes = Arrays.asList(SQLUtils.getSerialPrimaryKeyTypeString(conn), "TEXT", "INT");
		SQLUtils.createTable(conn, dbInfo.schema, table_hierarchy, columnNames, columnTypes);
	}
//	private void initCategoryIDSQLTable() throws SQLException, RemoteException
//	{
//		List<String> columnNames = new Vector<String>();
//		List<String> columnTypes = new Vector<String>();
//		/* Create a table for hierarchy (id, parent_id, title) */
//		
//		columnNames.clear();
//		columnNames.add(ID);
//		columnNames.add(PARENT_ID);
//		columnNames.add(TITLE);
//		columnTypes.clear();
//		columnTypes.add(SQLTYPE_VARCHAR);
//		SQLUtils.createTable(getConnection(), dbInfo.schema, TABLE_CATEGORY, columnNames, columnTypes);
//		
//	}
    public boolean isConnectedToDatabase()
    {
		return true;
    }
	synchronized public DatabaseConfigInfo getDatabaseConfigInfo() throws RemoteException
	{
		return connectionConfig.getDatabaseConfigInfo();
	}
	// these functions are just passed to the private connectionConfig
	public Document getDocument() throws RemoteException
	{
		return connectionConfig.getDocument();
	}

	public List<String> getConnectionNames() throws RemoteException
	{
		return connectionConfig.getConnectionNames();
	}
/* Private methods which handle the barebones of the entity-attribute-value system. */
	private String getDescription(int id) throws RemoteException
	{
		try
		{
			Connection conn = getConnection();
			String query = String.format(
				"SELECT %s FROM %s WHERE %s=?",
				SQLUtils.quoteSymbol(conn, DESCRIPTION),
				SQLUtils.quoteSchemaTable(conn, dbInfo.schema, table_desc),
				SQLUtils.quoteSymbol(conn, ID)
			);
			SQLResult result = SQLUtils.getRowSetFromQuery(conn, query, new String[]{Integer.toString(id)});
			if (result.rows.length > 0)
				return (String)result.rows[0][0];
			return null;
		}
		catch (SQLException e)
		{
			throw new RemoteException("Unable to get IDs from property table.", e);
		}
	}
    private List<Integer> getIdsFromMetadata(String sqlTable, Map<String,String> constraints) throws RemoteException
    {
        List<Integer> ids = new LinkedList<Integer>();
        try
        {
            Connection conn = getConnection();
            List<Map<String,String>> crossRowArgs = new LinkedList<Map<String,String>>();
            for (Entry<String,String> keyValPair : constraints.entrySet())
            {
                Map<String,String> colvalpair = new HashMap<String,String>();
                colvalpair.put(PROPERTY, keyValPair.getKey());
                colvalpair.put(VALUE, keyValPair.getValue());
                crossRowArgs.add(colvalpair);
            } 

            if (crossRowArgs.size() == 0)
            {
            	ids = SQLUtils.getIntColumn(conn, dbInfo.schema, table_desc, ID);
            }
            else
            {
            	ids = SQLUtils.crossRowSelect(conn, dbInfo.schema, sqlTable, ID, crossRowArgs);
            }
        }
        catch (SQLException e)
        {
            throw new RemoteException("Unable to get IDs from property table.", e);
        }
        return ids;
    }
    /**
     * @param id ID of an attribute column
     * @param properties A list of metadata property names to return
     * @return A map of the requested property names to values
     * @throws RemoteException
     */
    private Map<Integer,Map<String,String>> getMetadataFromIds(String sqlTable, Collection<Integer> ids, Collection<String> properties) throws RemoteException
    {
    	Map<Integer,Map<String,String>> results;
    	try
    	{
    		Connection conn = getConnection();
    		results = SQLUtils.idInSelect(conn, dbInfo.schema, sqlTable, ID, PROPERTY, VALUE, ids, properties);
    	}
    	catch (Exception e)
    	{
    		throw new RemoteException("Failed to get properties.", e);
    	}
    	return results; 
    }
    private void setMetadataProperty(String sqlTable, Integer id, String property, String value) throws RemoteException 
    {
        try {
            Connection conn = getConnection();
            
            // to overwrite metadata, first delete then insert
            Map<String,Object> delete_args = new HashMap<String,Object>();
            delete_args.put(PROPERTY, property);
            delete_args.put(ID, id);
            SQLUtils.deleteRows(conn, dbInfo.schema, sqlTable, delete_args);
            
            if (value != null && value.length() > 0)
            {
            	Map<String,Object> insert_args = new HashMap<String,Object>();
            	insert_args.put(PROPERTY, property);
            	insert_args.put(VALUE, value);
            	insert_args.put(ID, id);
            	SQLUtils.insertRow(conn, dbInfo.schema, sqlTable, insert_args);
            }
        }
        catch (Exception e)
        {
            throw new RemoteException("Failed to set property.", e);
        }
    }
    private void delEntry(Integer id) throws RemoteException
    {
        try {
            Connection conn = getConnection();
            Map<String,Object> whereParams = new HashMap<String,Object>();
            whereParams.put(ID, id);
            SQLUtils.deleteRows(conn, dbInfo.schema, table_meta_public, whereParams);
            SQLUtils.deleteRows(conn, dbInfo.schema, table_meta_private, whereParams);
            SQLUtils.deleteRows(conn, dbInfo.schema, table_desc, whereParams);
        }
        catch (Exception e)
        {
            throw new RemoteException("Failed to delete entry.", e);
        }
    }
/* ** END** Private methods which handle the barebones of the entity-attribute-value system. */
    
	public void addConnection(ConnectionInfo info) throws RemoteException
	{
		connectionConfig.addConnection(info);
	}

	public ConnectionInfo getConnectionInfo(String connectionName) throws RemoteException
	{
		return connectionConfig.getConnectionInfo(connectionName);
	}

	public void removeConnection(String name) throws RemoteException
	{
		connectionConfig.removeConnection(name);
	}

	/**
	 * This is a legacy interface for adding an attribute column. The id and description fields of the info object are not used.
	 */
	public int addAttributeColumn(AttributeColumnInfo info) throws RemoteException
	{
        int uniq_id = -1;
        try
        {
            Connection conn = getConnection();
            Map<String,Object> record = new HashMap<String,Object>();
            record.put(DESCRIPTION, info.description);
            uniq_id = SQLUtils.insertRowReturnID(conn, dbInfo.schema, table_desc, record);
	        
            for (Entry<String,String> entry : info.publicMetadata.entrySet())
	        	setMetadataProperty(table_meta_public, uniq_id, entry.getKey(), entry.getValue());
	        
            for (Entry<String,String> entry : info.privateMetadata.entrySet())
            	setMetadataProperty(table_meta_private, uniq_id, entry.getKey(), entry.getValue());
        }
        catch (Exception e)
        {
            throw new RemoteException("Unable to insert description item.",e);
        }
        return uniq_id;
	}

	/**
	 * @return A list of AttributeColumnInfo objects having info that matches the given parameters.
	 */
	public List<AttributeColumnInfo> getAttributeColumnInfo(Map<String, String> publicMetadataFilter) throws RemoteException
	{
		return getAttributeColumnInfo(publicMetadataFilter, null);
	}
	
	/**
	 * @return A list of AttributeColumnInfo objects having info that matches the given parameters.
	 */
	public List<AttributeColumnInfo> getAttributeColumnInfo(Map<String, String> publicMetadataFilter, Map<String, String> privateMetadataFilter) throws RemoteException
	{
		List<Integer> idList = getIdsFromMetadata(table_meta_public, publicMetadataFilter);
		if (privateMetadataFilter != null)
		{
			List<Integer> privateIdList = getIdsFromMetadata(table_meta_private, privateMetadataFilter);
			idList.retainAll(privateIdList);
		}
		
		List<AttributeColumnInfo> results = new Vector<AttributeColumnInfo>();
		Map<Integer, Map<String, String>> idToPrivateMeta = getMetadataFromIds(table_meta_private, idList, null);
		Map<Integer, Map<String, String>> idToPublicMeta = getMetadataFromIds(table_meta_public, idList, null);
		for (Integer id : idList)
		{
			AttributeColumnInfo info = new AttributeColumnInfo(id, getDescription(id));
			
			if (idToPrivateMeta.containsKey(id))
				info.privateMetadata = idToPrivateMeta.get(id);
			else
				info.privateMetadata = new HashMap<String,String>();
			
			if (idToPublicMeta.containsKey(id))
				info.publicMetadata = idToPublicMeta.get(id);
			else
				info.publicMetadata = new HashMap<String,String>();
			
			results.add(info);
		}
		return results;
	}
	
	/**
	 * @param id The ID of an attribute column.
	 * @return The AttributeColumnInfo object identified by the id, or null if it doesn't exist.
	 * @throws RemoteException
	 */
	public AttributeColumnInfo getAttributeColumnInfo(int id) throws RemoteException
	{
		AttributeColumnInfo info = new AttributeColumnInfo(id, getDescription(id));
		
		List<Integer> idList = new Vector<Integer>();
		idList.add(id);
		
		Map<Integer, Map<String, String>> idToPrivateMeta = getMetadataFromIds(table_meta_private, idList, null);
		if (idToPrivateMeta.containsKey(id))
			info.privateMetadata = idToPrivateMeta.get(id);
		else
			info.privateMetadata = new HashMap<String,String>();
		
		Map<Integer, Map<String, String>> idToPublicMeta = getMetadataFromIds(table_meta_public, idList, null);
		if (idToPublicMeta.containsKey(id))
			info.publicMetadata = idToPublicMeta.get(id);
		else
			info.publicMetadata = new HashMap<String,String>();
		
		return info;
	}
	
	/**
	 * @param id The ID of the attribute column entry to remove.
	 * @throws RemoteException
	 */
	public void removeAttributeColumnInfo(int id) throws RemoteException
	{
		delEntry(id);
	}
}



