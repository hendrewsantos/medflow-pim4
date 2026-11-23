package br.com.medflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.medflow.app.ui.MedFlowRoot
import br.com.medflow.app.ui.theme.MedFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MedFlowApp).container
        setContent {
            MedFlowTheme {
                MedFlowRoot(container)
            }
        }
    }
}
