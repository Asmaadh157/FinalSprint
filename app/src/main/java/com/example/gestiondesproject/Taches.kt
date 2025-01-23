package com.example.gestiondesproject

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class Taches : AppCompatActivity() {

    private lateinit var editTextTaskTitle: EditText
    private lateinit var textViewSelectedFile: TextView
    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private var selectedFileUri: Uri? = null
    private lateinit var groupId: String
    private lateinit var status:String
    private val database = FirebaseDatabase.getInstance().getReference("Groupes")

    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.taches)

        editTextTaskTitle = findViewById(R.id.editTextTaskTitle)
        textViewSelectedFile = findViewById(R.id.textViewSelectedFile)
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)

        val buttonChooseFile: Button = findViewById(R.id.buttonChooseFile)
        val buttonAddTask: Button = findViewById(R.id.buttonAddTask)
        val buttonShowForm: ImageButton = findViewById(R.id.buttonShowForm)

        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                selectedFileUri = result.data?.data
                textViewSelectedFile.text = selectedFileUri?.lastPathSegment ?: "Fichier sélectionné"
                // Pass the selected file URI to the adapter
                selectedFileUri?.let { taskAdapter.updateSelectedFileUri(it) }
            }
        }
        groupId = intent.getStringExtra("groupeId") ?: ""
        status = intent.getStringExtra("status") ?: "Status non défini"


        recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(
            mutableListOf(),
            this,
            status,
        ) { fileUri -> openFile(fileUri) }
        taskAdapter.setFilePickerLauncher(filePickerLauncher)
        recyclerViewTasks.adapter = taskAdapter


        if (groupId.isNotEmpty()) {
            recupererTaches(groupId)
        }

        buttonChooseFile.setOnClickListener {
            openFilePicker()
        }

        buttonAddTask.setOnClickListener {
            val taskTitle = editTextTaskTitle.text.toString().trim()

            if (taskTitle.isNotEmpty()) {
                val taskFileUri = selectedFileUri?.toString() ?: ""

                val newTask = Task(taskTitle, taskFileUri, groupId = groupId)
                addTaskToFirebase(groupId, newTask)

                editTextTaskTitle.text.clear()
                textViewSelectedFile.text = "Aucun fichier sélectionné"
                selectedFileUri = null

                editTextTaskTitle.visibility = View.GONE
                buttonChooseFile.visibility = View.GONE
                textViewSelectedFile.visibility = View.GONE
                buttonAddTask.visibility = View.GONE

                Toast.makeText(this, "Tâche ajoutée avec succès", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Veuillez entrer un titre", Toast.LENGTH_SHORT).show()
            }
        }

        buttonShowForm.setOnClickListener {
            buttonShowForm.setOnClickListener {
                showAddTaskDialog()
            }
        }
        if( status != "Étudiant"){
            buttonShowForm.visibility = View.VISIBLE
        }else{
            buttonShowForm.visibility = View.GONE
        }
        // Récupération du bouton de retour
        val buttonBack: ImageButton = findViewById(R.id.BackButton2)

        buttonBack.setOnClickListener {
            // Appel à onBackPressed() pour simuler un appui sur le bouton retour du téléphone
            onBackPressed()
        }

    }

    private fun showAddTaskDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_tache, null)
        val editTextTitle = dialogView.findViewById<EditText>(R.id.editTextTaskTitle)
        val textViewSelectedFile = dialogView.findViewById<TextView>(R.id.textViewSelectedFile)
        val buttonChooseFile = dialogView.findViewById<ImageButton>(R.id.imageButtonChooseFile)
        val buttonAddTask = dialogView.findViewById<Button>(R.id.buttonAddTask)

        buttonChooseFile.setOnClickListener {
            openFilePicker()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Ajouter une tâche")
            .setView(dialogView)
            .setNegativeButton("Annuler", null)
            .create()

        buttonAddTask.setOnClickListener {
            val taskTitle = editTextTitle.text.toString().trim()

            if (taskTitle.isNotEmpty()) {
                val taskFileUri = selectedFileUri?.toString() ?: ""

                val newTask = Task(taskTitle, taskFileUri, groupId = groupId)
                addTaskToFirebase(groupId, newTask)

                editTextTitle.text.clear()
                textViewSelectedFile.text = "Aucun fichier sélectionné"
                selectedFileUri = null

                dialog.dismiss()  // Close the dialog
                Toast.makeText(this, "Tâche ajoutée avec succès", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Veuillez entrer un titre", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }

    private fun openFile(fileUri: String) {
        val uri = Uri.parse(fileUri)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, contentResolver.getType(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    private fun recupererTaches(groupId: String) {
        val tasksRef = database.child(groupId).child("Taches")
        tasksRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<Task>()
                for (taskSnapshot in snapshot.children) {
                    val taskTitle = taskSnapshot.child("title").getValue(String::class.java)
                    val taskFileUri = taskSnapshot.child("fileUri").getValue(String::class.java)
                    val taskStatus = taskSnapshot.child("status").getValue(String::class.java) // Get the task status
                    val taskId = taskSnapshot.key
                    if (taskTitle != null && taskFileUri != null && taskId != null) {
                        val task = Task(taskTitle, taskFileUri, taskId, groupId, taskStatus ?: "default_status") // Include status
                        tasks.add(task)
                    }
                }
                taskAdapter.updateTaskList(tasks)
                taskAdapter.notifyDataSetChanged() // Ensure the view is refreshed
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Taches, "Erreur lors de la récupération des tâches", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun addTaskToFirebase(groupId: String, task: Task) {
        val taskRef = database.child(groupId).child("Taches").push()
        taskRef.setValue(task).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "Tâche ajoutée avec succès", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Erreur lors de l'ajout de la tâche", Toast.LENGTH_SHORT).show()
            }
        }
    }
}