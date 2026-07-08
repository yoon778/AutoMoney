package com.choiyoonseo.automoney.security

import com.google.common.truth.Truth.assertThat
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Test
import org.w3c.dom.Element

class SecurityManifestTest {
    @Test
    fun appDisablesAndroidBackupForFinancialData() {
        val manifest = File("src/main/AndroidManifest.xml")
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(manifest)
        val application = document
            .getElementsByTagName("application")
            .item(0) as Element

        assertThat(application.getAttributeNS(ANDROID_NAMESPACE, "allowBackup")).isEqualTo("false")
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
