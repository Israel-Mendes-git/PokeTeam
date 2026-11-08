import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Versão: vem da tag no GitHub Actions (VERSAO=1.2.3) ou de -Pversao=1.2.3; senão o padrão abaixo.
 * versionCode = maior*10000 + menor*100 + correção, então "1.2.3" > "1.2.2" sempre.
 */
val versao: String = (System.getenv("VERSAO") ?: findProperty("versao") as String? ?: "0.2.0").removePrefix("v")
val codigoVersao: Int = versao.split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
    .let { (it + listOf(0, 0, 0)).take(3) }.let { (a, b, c) -> a * 10000 + b * 100 + c }

/** Repositório onde o app procura atualizações ("dono/nome"). Forks trocam em gradle.properties. */
val repoGithub: String = findProperty("poketeam.repo") as String? ?: ""
/**
 * Servidor de amigos e ID "Web" do Google: config.properties na raiz (fora do Git) ou variáveis de
 * ambiente no GitHub Actions. Sem eles, a aba Amigos explica que o recurso está desligado neste build.
 * Teste local: -PservidorTeste=http://10.0.2.2:8787 (servidor do wrangler dev, com login falso).
 */
val config = Properties().apply {
    rootProject.file("config.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val servidor: String = findProperty("servidorTeste") as String?
    ?: System.getenv("POKETEAM_SERVIDOR") ?: config.getProperty("servidor") ?: ""
val googleWebClientId: String = System.getenv("POKETEAM_GOOGLE_CLIENT_ID") ?: config.getProperty("googleWebClientId") ?: ""

/** Só para teste local: -PapiGithub=http://10.0.2.2:8765 aponta para um servidor falso. */
val apiGithub: String = findProperty("apiGithub") as String? ?: "https://api.github.com"

/** Chave de release: variáveis de ambiente (CI) ou keystore.properties na raiz (fora do Git). */
val chave = Properties().apply {
    val arq = rootProject.file("keystore.properties")
    if (arq.exists()) arq.inputStream().use(::load)
    System.getenv("POKETEAM_KEYSTORE")?.let { setProperty("storeFile", it) }
    System.getenv("POKETEAM_KEYSTORE_SENHA")?.let { setProperty("storePassword", it); setProperty("keyPassword", it) }
    System.getenv("POKETEAM_KEY_ALIAS")?.let { setProperty("keyAlias", it) }
}
val temChave = chave.getProperty("storeFile")?.let { file(it).exists() } == true

android {
    namespace = "com.poketeam.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.poketeam.app"
        minSdk = 26
        targetSdk = 36
        versionCode = codigoVersao
        versionName = versao
        // Teste local: -PsufixoTeste instala como outro app, sem tocar no instalado.
        findProperty("sufixoTeste")?.let { applicationIdSuffix = ".teste" }
        buildConfigField("String", "REPO_GITHUB", "\"$repoGithub\"")
        buildConfigField("String", "API_GITHUB", "\"$apiGithub\"")
        buildConfigField("String", "SERVIDOR", "\"$servidor\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        // Servidores de teste locais são http; em produção só https.
        manifestPlaceholders["permiteHttp"] = (apiGithub.startsWith("http://") || servidor.startsWith("http://")).toString()
    }

    signingConfigs {
        create("release") {
            if (temChave) {
                storeFile = file(chave.getProperty("storeFile"))
                storePassword = chave.getProperty("storePassword")
                keyAlias = chave.getProperty("keyAlias")
                keyPassword = chave.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8: encolhe e otimiza (Compose fica bem mais leve no release).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Atualização automática exige sempre a mesma chave. Sem ela (clone novo), cai na de debug.
            signingConfig = signingConfigs.getByName(if (temChave) "release" else "debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        // App pessoal: sem lint no build de release (evita baixar ~500 MB de ferramentas).
        checkReleaseBuilds = false
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.credentials)
    implementation(libs.credentials.play)
    implementation(libs.googleid)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
