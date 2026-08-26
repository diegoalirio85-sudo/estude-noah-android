package com.estudenoah.app.ui.parents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackendAccountScreen(onBack: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Conta do backend", fontWeight = FontWeight.Bold) }, navigationIcon = { TextButton(onClick = onBack) { Text("← Voltar") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (user == null) "Desconectado" else "Conectado", fontWeight = FontWeight.Bold)
            user?.email?.let { Text(it) }
            if (user == null) {
                OutlinedTextField(email, { email = it.trim(); message = null }, label = { Text("Email") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it; message = null }, label = { Text("Senha") }, singleLine = true, enabled = !busy, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    busy = true
                    message = null
                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        password = ""
                        busy = false
                        if (task.isSuccessful) { user = auth.currentUser; message = "Conta conectada." }
                        else message = "Não foi possível entrar. Confira email e senha."
                    }
                }, enabled = !busy && email.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Entrando…" else "Entrar") }
            } else {
                OutlinedButton(onClick = { auth.signOut(); user = null; email = ""; password = ""; message = "Conta desconectada." }, modifier = Modifier.fillMaxWidth()) { Text("Sair") }
            }
            message?.let { Text(it) }
            Text("A senha é entregue diretamente ao Firebase e não é salva pelo Estude, Noah!.")
        }
    }
}

