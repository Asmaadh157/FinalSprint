package com.example.gestiondesproject

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class TaskAdapter(
    private val taskList: MutableList<Task>,
    private val context: Context,
    private val status: String, // New parameter
    private val openFile: (String) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var filePickerLauncher: ActivityResultLauncher<Intent>? = null
    private var currentSelectedFileUri: Uri? = null

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskTitle: TextView = itemView.findViewById(R.id.textViewTaskTitle)
        val taskFile: TextView = itemView.findViewById(R.id.textViewTaskFile)
        val taskStatusCircle: View = itemView.findViewById(R.id.viewTaskStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.taskTitle.text = task.title
        holder.taskFile.text = if (task.fileUri.isNotEmpty()) "Fichier: ${task.fileUri}" else "Aucun fichier"

        // Set the color of the status circle based on the task status
        when (task.status) {
            "accepted" -> holder.taskStatusCircle.setBackgroundColor(Color.GREEN)
            "rejected" -> holder.taskStatusCircle.setBackgroundColor(Color.RED)
            "modified" -> holder.taskStatusCircle.setBackgroundColor(Color.rgb(255, 165, 0)) // Orange color

            else -> holder.taskStatusCircle.setBackgroundColor(Color.GRAY) // Default status
        }

        holder.taskStatusCircle.setOnClickListener {
            if (status != "Étudiant") {
                showStatusMenu(holder.taskStatusCircle, task, position)
            } else {
                Toast.makeText(context, "Les étudiants n'ont pas accès à cette option", Toast.LENGTH_SHORT).show()
            }
        }

        holder.taskFile.setOnClickListener {
            try {
                if (task.fileUri.isNotEmpty()) {
                    openFile(task.fileUri)
                } else {
                    Toast.makeText(context, "Aucun fichier disponible", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()  // Log the exception for debugging
            }
        }

        holder.taskTitle.setOnClickListener {
            if (status == "Étudiant") {
                // If the user is a student, show the file upload dialog
                showFileUploadDialog(task, position)
            } else {
                // If the user is a professor, show the task modify/delete menu
                showFileMenu(holder.taskTitle, task, position)
            }
        }


    }
    private fun showFileUploadDialog(task: Task, position: Int) {
        // Create the dialog layout
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_upload_file, null)
        val buttonChooseFile = dialogView.findViewById<Button>(R.id.buttonChooseFile)
        val textViewSelectedFile = dialogView.findViewById<TextView>(R.id.textViewSelectedFile)

        // Set the current file (if exists) to show it in the dialog
        textViewSelectedFile.text = if (task.fileUri.isNotEmpty()) "Fichier actuel: ${task.fileUri}" else "Aucun fichier sélectionné"
        buttonChooseFile.setOnClickListener {
            filePickerLauncher?.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            })
        }
        val customTitle = TextView(context)
        customTitle.text = "Modifier la tâche"
        customTitle.textSize = 20f
        customTitle.setTextColor(ContextCompat.getColor(context, R.color.app_color))
        customTitle.typeface = ResourcesCompat.getFont(context, R.font.roboto_regular)
        customTitle.setPadding(40, 40, 40, 0)
        customTitle.gravity = Gravity.CENTER
        // Create the dialog
        val dialog = android.app.AlertDialog.Builder(context)
            .setCustomTitle(customTitle)
            .setView(dialogView)
            .setPositiveButton("Confirmer") { _, _ ->
                val updatedFileUri = currentSelectedFileUri?.toString() ?: task.fileUri

                if (updatedFileUri.isNotEmpty()) {
                    val updatedTask = task.copy( fileUri = updatedFileUri)
                    updateTaskInFirebase(task.id ?: "", updatedTask, position)
                } else {
                    Toast.makeText(context, "Veuillez entrer un titre valide", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .create()

        dialog.setOnShowListener {
            val robotoTypeface = ResourcesCompat.getFont(context, R.font.roboto_regular)

            val confirmButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            val cancelButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)

            confirmButton?.typeface = robotoTypeface
            cancelButton?.typeface = robotoTypeface
        }

        dialog.show()
    }


    private fun uploadFileToFirebase(task: Task, position: Int) {
        val storageReference = FirebaseStorage.getInstance().getReference("task_files")
        val fileReference = storageReference.child("${task.id}_${System.currentTimeMillis()}")

        // Check if there's an existing file and delete it if it exists
        if (task.fileUri.isNotEmpty()) {
            val oldFileReference = FirebaseStorage.getInstance().getReferenceFromUrl(task.fileUri)
            oldFileReference.delete()
                .addOnSuccessListener {
                    // Proceed with the upload if the old file is successfully deleted
                    uploadNewFile(fileReference, task, position)
                }
                .addOnFailureListener {
                    // In case the old file deletion failed, proceed with the new file upload
                    uploadNewFile(fileReference, task, position)
                }
        } else {
            // No existing file, just upload the new file
            uploadNewFile(fileReference, task, position)
        }
    }

    private fun uploadNewFile(fileReference: StorageReference, task: Task, position: Int) {
        // Upload the new file
        fileReference.putFile(currentSelectedFileUri!!)
            .addOnSuccessListener { taskSnapshot ->
                fileReference.downloadUrl.addOnSuccessListener { uri ->
                    val fileUrl = uri.toString()

                    // Update the file URL in Firebase Realtime Database
                    val taskRef = FirebaseDatabase.getInstance().getReference("Groupes")
                        .child(task.groupId).child("Taches").child(task.id ?: "")

                    taskRef.child("fileUri").setValue(fileUrl).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            task.fileUri = fileUrl
                            taskList[position] = task
                            notifyItemChanged(position)
                            Toast.makeText(context, "Fichier téléchargé avec succès", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Erreur lors de l'ajout du fichier", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Échec du téléchargement du fichier", Toast.LENGTH_SHORT).show()
            }
    }



    private fun showStatusMenu(view: View, task: Task, position: Int) {
        val popupMenu = PopupMenu(context, view)
        if (status != "Étudiant") {
            popupMenu.menuInflater.inflate(R.menu.task_status_menu, popupMenu.menu)
        }
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_accept -> {
                    updateTaskStatus(task, "accepted", position)
                    true
                }
                R.id.menu_reject -> {
                    updateTaskStatus(task, "rejected", position)
                    true
                }
                R.id.menu_modify -> {
                    updateTaskStatus(task, "modified", position)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }


    private fun showFileMenu(view: View, task: Task, position: Int) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.menuInflater.inflate(R.menu.task_context_menu, popupMenu.menu)  // Assuming you have a menu for file actions

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_modify -> {
                    showModifyDialog(task, position)
                    true
                }
                R.id.menu_delete -> {
                    deleteTask(task, position)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }


    private fun updateTaskStatus(task: Task, newStatus: String, position: Int) {
        task.status = newStatus
        val taskRef = FirebaseDatabase.getInstance().getReference("Groupes")
            .child(task.groupId).child("Taches").child(task.id ?: "")

        taskRef.child("status").setValue(newStatus).addOnCompleteListener { taskUpdate ->
            if (taskUpdate.isSuccessful) {
                taskList[position] = task
                notifyItemChanged(position)
                Toast.makeText(context, "Statut mis à jour avec succès", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Erreur lors de la mise à jour du statut", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showModifyDialog(task: Task, position: Int) {
        currentSelectedFileUri = null // Reset the selected file URI

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_modify_task, null)
        val editTextTitle = dialogView.findViewById<EditText>(R.id.editTextModifyTitle)
        val textViewSelectedFile = dialogView.findViewById<TextView>(R.id.textViewModifySelectedFile)
        val buttonChooseFile = dialogView.findViewById<Button>(R.id.buttonModifyChooseFile)

        editTextTitle.setText(task.title)
        textViewSelectedFile.text = task.fileUri.ifEmpty { "Aucun fichier sélectionné" }

        buttonChooseFile.setOnClickListener {
            filePickerLauncher?.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            })
        }
        val customTitle = TextView(context)
        customTitle.text = "Modifier la tâche"
        customTitle.textSize = 20f
        customTitle.setTextColor(ContextCompat.getColor(context, R.color.app_color))
        customTitle.typeface = ResourcesCompat.getFont(context, R.font.roboto_regular)
        customTitle.setPadding(40, 40, 40, 0)
        customTitle.gravity = Gravity.CENTER

        val dialog = android.app.AlertDialog.Builder(context)
            .setCustomTitle(customTitle)
            .setView(dialogView)
            .setPositiveButton("Confirmer") { _, _ ->
                val updatedTitle = editTextTitle.text.toString().trim()
                val updatedFileUri = currentSelectedFileUri?.toString() ?: task.fileUri

                if (updatedTitle.isNotEmpty()) {
                    val updatedTask = task.copy(title = updatedTitle, fileUri = updatedFileUri)
                    updateTaskInFirebase(task.id ?: "", updatedTask, position)
                } else {
                    Toast.makeText(context, "Veuillez entrer un titre valide", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .create()
        dialog.setOnShowListener {
            val robotoTypeface = ResourcesCompat.getFont(context, R.font.roboto_regular)

            val confirmButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            val cancelButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)

            confirmButton?.typeface = robotoTypeface
            cancelButton?.typeface = robotoTypeface
        }

        dialog.show()
    }


    fun setFilePickerLauncher(launcher: ActivityResultLauncher<Intent>) {
        filePickerLauncher = launcher
    }

    fun updateSelectedFileUri(uri: Uri?) {
        currentSelectedFileUri = uri
    }

    fun updateTaskList(newTaskList: List<Task>) {
        taskList.clear()
        taskList.addAll(newTaskList)
    }

    private fun updateTaskInFirebase(taskId: String, updatedTask: Task, position: Int) {
        val taskRef = FirebaseDatabase.getInstance().getReference("Groupes")
            .child(updatedTask.groupId).child("Taches").child(taskId)

        taskRef.setValue(updatedTask).addOnCompleteListener { taskUpdate ->
            if (taskUpdate.isSuccessful) {
                taskList[position] = updatedTask
                notifyItemChanged(position)
                Toast.makeText(context, "Tâche modifiée avec succès", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Erreur lors de la modification de la tâche", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteTask(task: Task, position: Int) {
        val taskRef = FirebaseDatabase.getInstance().getReference("Groupes")
            .child(task.groupId).child("Taches").child(task.id ?: "")

        taskRef.removeValue().addOnCompleteListener { taskRemoval ->
            if (taskRemoval.isSuccessful) {
                if (position >= 0 && position < taskList.size) {
                    taskList.removeAt(position)

                    Toast.makeText(context, "Tâche supprimée avec succès", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Erreur : position invalide", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Erreur lors de la suppression de la tâche", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Échec de la suppression de la tâche", Toast.LENGTH_SHORT).show()
        }
    }



    override fun getItemCount(): Int {
        return taskList.size
    }
}