package test;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Suite Completa de Pruebas - Sistema Clínica")
@SelectClasses({
    AsociadoDTOTest.class,
    PersistenciaIntegracionTest.class,
    AmbulanciaMockConcurrenciaTest.class
})
public class TestSuite {}