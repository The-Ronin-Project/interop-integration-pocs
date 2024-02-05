plugins {
    alias(libs.plugins.interop.spring.boot)
}

dependencies {
    implementation(libs.soda)
    implementation(libs.interop.ehr.ronin.generators)
    runtimeOnly(libs.liquibase.core)
    implementation(libs.interop.ehr.liquibase)
    implementation(libs.ronin.test.data.generator)
    implementation(libs.interop.common.jackson)
    implementation(libs.interop.fhir)
    implementation(libs.interop.fhirGenerators)

    implementation(libs.bundles.ojdbc)
    implementation(libs.bundles.oracle.security)

    implementation(platform(libs.spring.boot.parent))
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
}
