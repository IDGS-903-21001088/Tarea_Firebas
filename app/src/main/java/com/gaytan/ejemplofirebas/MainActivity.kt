package com.gaytan.ejemplofirebas

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var btnGuardar: Button
    private lateinit var tvResultado: TextView

    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etNombre = findViewById(R.id.etNombre)
        btnGuardar = findViewById(R.id.btnGuardar)
        tvResultado = findViewById(R.id.tvResultado)

        dbRef = FirebaseDatabase.getInstance().getReference("usuarios")

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString()
            if (nombre.isNotEmpty()) {
                val id = dbRef.push().key
                id?.let {
                    dbRef.child(it).setValue(nombre)
                }
                etNombre.text.clear()
            }
        }

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val builder = StringBuilder()
                for (userSnapshot in snapshot.children) {
                    val nombre = userSnapshot.getValue(String::class.java)
                    builder.append("- ").append(nombre).append("\n")
                }
                tvResultado.text = builder.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                tvResultado.text = "Error al leer la base de datos"
            }
        })
    }
}
