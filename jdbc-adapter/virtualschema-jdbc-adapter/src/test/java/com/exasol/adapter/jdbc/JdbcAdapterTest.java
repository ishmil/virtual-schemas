package com.exasol.adapter.jdbc;

import static com.exasol.adapter.AdapterProperties.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.capabilities.MainCapability;
import com.exasol.adapter.metadata.SchemaMetadataInfo;
import com.exasol.adapter.metadata.TableMetadata;
import com.exasol.adapter.request.*;
import com.exasol.adapter.response.*;
import com.exasol.adapter.sql.SqlStatement;
import com.exasol.adapter.sql.TestSqlStatementFactory;

class JdbcAdapterTest {
    private static final String SCHEMA_NAME = "THE_SCHEMA";
    private static final String GENERIC_ADAPTER_NAME = "GENERIC";
    private final JdbcAdapter adapter = new JdbcAdapter();
    private Map<String, String> rawProperties;

    @BeforeEach
    void beforeEach() {
        this.rawProperties = new HashMap<>();
    }

    @Test
    void testPushdown() throws AdapterException {
        final PushDownResponse response = pushStatementDown(TestSqlStatementFactory.createSelectOneFromSysDummy());
        assertThat(response.getPushDownSql(), equalTo("IMPORT INTO (c1 DECIMAL(10, 0))" //
                + " FROM JDBC" //
                + " AT 'jdbc:derby:memory:test;create=true;' USER '' IDENTIFIED BY ''"//
                + " STATEMENT 'SELECT 1 FROM \"SYSIBM\".\"SYSDUMMY1\"'"));
    }

    private PushDownResponse pushStatementDown(final SqlStatement statement) throws AdapterException {
        setGenericSqlDialectProperty();
        setDerbyConnectionProperties();
        this.rawProperties.put(SCHEMA_NAME_PROPERTY, "SYSIBM");
        final List<TableMetadata> involvedTablesMetadata = null;
        final PushDownRequest request = new PushDownRequest(GENERIC_ADAPTER_NAME, createSchemaMetadataInfo(), statement,
                involvedTablesMetadata);
        final ExaMetadata exaMetadataMock = Mockito.mock(ExaMetadata.class);
        final PushDownResponse response = this.adapter.pushdown(exaMetadataMock, request);
        return response;
    }

    private void setGenericSqlDialectProperty() {
        this.rawProperties.put(SQL_DIALECT_PROPERTY, GENERIC_ADAPTER_NAME);
    }

    private void setDerbyConnectionProperties() {
        this.rawProperties.put(CONNECTION_STRING_PROPERTY, "jdbc:derby:memory:test;create=true;");
        this.rawProperties.put(USERNAME_PROPERTY, "");
        this.rawProperties.put(PASSWORD_PROPERTY, "");
    }

    private SchemaMetadataInfo createSchemaMetadataInfo() {
        return new SchemaMetadataInfo(SCHEMA_NAME, "", this.rawProperties);
    }

    @Test
    void testPushdownWithIllegalStatementThrowsException() {
        assertThrows(RemoteMetadataReaderException.class,
                () -> pushStatementDown(TestSqlStatementFactory.createSelectOneFromDual()));
    }

    @Test
    void testGetCapabilities() throws AdapterException {
        setGenericSqlDialectProperty();
        setDerbyConnectionProperties();
        this.rawProperties.put(SCHEMA_NAME_PROPERTY, "SYSIBM");
        final GetCapabilitiesRequest request = new GetCapabilitiesRequest(GENERIC_ADAPTER_NAME,
                createSchemaMetadataInfo());
        final ExaMetadata exaMetadataMock = Mockito.mock(ExaMetadata.class);
        final GetCapabilitiesResponse response = this.adapter.getCapabilities(exaMetadataMock, request);
        assertThat(response.getCapabilities().getMainCapabilities(), emptyCollectionOf(MainCapability.class));
    }

    @Test
    void testDropVirtualSchemaMustSucceedEvenIfDebugAddressIsInvalid() throws AdapterException {
        setGenericSqlDialectProperty();
        setDerbyConnectionProperties();
        final ExaMetadata exaMetadataMock = Mockito.mock(ExaMetadata.class);
        this.rawProperties.put(AdapterProperties.DEBUG_ADDRESS_PROPERTY, "this_is_an:invalid_debug_address");
        final DropVirtualSchemaRequest dropRequest = new DropVirtualSchemaRequest(GENERIC_ADAPTER_NAME,
                createSchemaMetadataInfo());
        final DropVirtualSchemaResponse response = this.adapter.dropVirtualSchema(exaMetadataMock, dropRequest);
        assertThat(response, notNullValue());
    }
}