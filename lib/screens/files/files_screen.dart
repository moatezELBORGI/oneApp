import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/document_provider.dart';
import '../../utils/app_theme.dart';
import '../../models/folder_model.dart';
import '../../models/document_model.dart';
import '../../services/document_service.dart';
import 'dart:io';

class FilesScreen extends StatefulWidget {
  const FilesScreen({super.key});

  @override
  State<FilesScreen> createState() => _FilesScreenState();
}

class _FilesScreenState extends State<FilesScreen> {
  final DocumentService _documentService = DocumentService();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<DocumentProvider>().loadRootFolders();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<DocumentProvider>(
      builder: (context, documentProvider, child) {
        return Scaffold(
          backgroundColor: AppTheme.backgroundColor,
          appBar: AppBar(
            title: Text(
              documentProvider.currentFolder?.name ?? 'Mes Documents',
              style: const TextStyle(
                fontWeight: FontWeight.w600,
                fontSize: 18,
              ),
            ),
            backgroundColor: Colors.white,
            elevation: 0,
            leading: documentProvider.canGoBack
                ? IconButton(
                    icon: const Icon(Icons.arrow_back),
                    onPressed: () {
                      documentProvider.goBack();
                    },
                  )
                : null,
            actions: [
              IconButton(
                icon: const Icon(Icons.search),
                onPressed: () {
                  _showSearchDialog();
                },
              ),
              IconButton(
                icon: const Icon(Icons.refresh),
                onPressed: () {
                  documentProvider.refresh();
                },
              ),
            ],
          ),
          body: _buildBody(documentProvider),
          floatingActionButton: Column(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              FloatingActionButton(
                heroTag: "create_folder_fab",
                onPressed: () {
                  _showCreateFolderDialog();
                },
                backgroundColor: AppTheme.primaryColor,
                child: const Icon(Icons.create_new_folder),
              ),
              const SizedBox(height: 16),
              FloatingActionButton(
                heroTag: "upload_file_fab",
                onPressed: () {
                  _showUploadFileDialog();
                },
                backgroundColor: AppTheme.accentColor,
                child: const Icon(Icons.upload_file),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildBody(DocumentProvider documentProvider) {
    if (documentProvider.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (documentProvider.error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 64, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              'Erreur',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: Colors.grey[700],
              ),
            ),
            const SizedBox(height: 8),
            Text(
              documentProvider.error!,
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey[600]),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: () {
                documentProvider.clearError();
                documentProvider.refresh();
              },
              child: const Text('Réessayer'),
            ),
          ],
        ),
      );
    }

    final folders = documentProvider.folders;
    final documents = documentProvider.documents;

    if (folders.isEmpty && documents.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.folder_open, size: 64, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              'Aucun fichier',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: Colors.grey[700],
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Créez un dossier ou uploadez un fichier',
              style: TextStyle(color: Colors.grey[600]),
            ),
          ],
        ),
      );
    }

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (folders.isNotEmpty) ...[
          const Text(
            'Dossiers',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 12),
          ...folders.map((folder) => _buildFolderCard(folder, documentProvider)),
          const SizedBox(height: 24),
        ],
        if (documents.isNotEmpty) ...[
          const Text(
            'Fichiers',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 12),
          ...documents.map((document) => _buildDocumentCard(document, documentProvider)),
        ],
      ],
    );
  }

  Widget _buildFolderCard(FolderModel folder, DocumentProvider provider) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: ListTile(
        leading: Container(
          width: 48,
          height: 48,
          decoration: BoxDecoration(
            color: AppTheme.primaryColor.withOpacity(0.1),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(
            Icons.folder,
            color: AppTheme.primaryColor,
            size: 28,
          ),
        ),
        title: Text(
          folder.name,
          style: const TextStyle(
            fontWeight: FontWeight.w600,
          ),
        ),
        subtitle: Text(
          '${folder.subFolderCount} dossiers • ${folder.documentCount} fichiers',
          style: TextStyle(
            color: Colors.grey[600],
            fontSize: 12,
          ),
        ),
        trailing: PopupMenuButton(
          itemBuilder: (context) => [
            const PopupMenuItem(
              value: 'delete',
              child: Row(
                children: [
                  Icon(Icons.delete, size: 18, color: Colors.red),
                  SizedBox(width: 8),
                  Text('Supprimer', style: TextStyle(color: Colors.red)),
                ],
              ),
            ),
          ],
          onSelected: (value) {
            if (value == 'delete') {
              _confirmDeleteFolder(folder, provider);
            }
          },
        ),
        onTap: () {
          provider.openFolder(folder);
        },
      ),
    );
  }

  Widget _buildDocumentCard(DocumentModel document, DocumentProvider provider) {
    IconData icon;
    Color color;

    if (document.isImage) {
      icon = Icons.image;
      color = Colors.blue;
    } else if (document.isPdf) {
      icon = Icons.picture_as_pdf;
      color = Colors.red;
    } else if (document.isDocument) {
      icon = Icons.description;
      color = Colors.orange;
    } else {
      icon = Icons.insert_drive_file;
      color = Colors.grey;
    }

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: ListTile(
        leading: Container(
          width: 48,
          height: 48,
          decoration: BoxDecoration(
            color: color.withOpacity(0.1),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(
            icon,
            color: color,
            size: 24,
          ),
        ),
        title: Text(
          document.originalFilename,
          style: const TextStyle(
            fontWeight: FontWeight.w600,
          ),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              document.getFormattedSize(),
              style: TextStyle(
                color: Colors.grey[600],
                fontSize: 12,
              ),
            ),
            if (document.description != null && document.description!.isNotEmpty)
              Text(
                document.description!,
                style: TextStyle(
                  color: Colors.grey[500],
                  fontSize: 11,
                ),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
          ],
        ),
        trailing: PopupMenuButton(
          itemBuilder: (context) => [
            const PopupMenuItem(
              value: 'download',
              child: Row(
                children: [
                  Icon(Icons.download, size: 18),
                  SizedBox(width: 8),
                  Text('Télécharger'),
                ],
              ),
            ),
            const PopupMenuItem(
              value: 'delete',
              child: Row(
                children: [
                  Icon(Icons.delete, size: 18, color: Colors.red),
                  SizedBox(width: 8),
                  Text('Supprimer', style: TextStyle(color: Colors.red)),
                ],
              ),
            ),
          ],
          onSelected: (value) {
            if (value == 'delete') {
              _confirmDeleteDocument(document, provider);
            } else if (value == 'download') {
              _downloadDocument(document);
            }
          },
        ),
        onTap: () {
          _previewDocument(document);
        },
      ),
    );
  }

  void _showCreateFolderDialog() {
    final nameController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Nouveau dossier'),
        content: TextField(
          controller: nameController,
          decoration: const InputDecoration(
            labelText: 'Nom du dossier',
            border: OutlineInputBorder(),
          ),
          autofocus: true,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Annuler'),
          ),
          ElevatedButton(
            onPressed: () {
              if (nameController.text.trim().isNotEmpty) {
                context
                    .read<DocumentProvider>()
                    .createFolder(nameController.text.trim());
                Navigator.pop(context);
              }
            },
            child: const Text('Créer'),
          ),
        ],
      ),
    );
  }

  void _showUploadFileDialog() async {
    final file = await _documentService.pickFile();
    if (file == null) return;

    if (!mounted) return;

    final descriptionController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Uploader un fichier'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Fichier: ${file.path.split('/').last}'),
            const SizedBox(height: 16),
            TextField(
              controller: descriptionController,
              decoration: const InputDecoration(
                labelText: 'Description (optionnel)',
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Annuler'),
          ),
          ElevatedButton(
            onPressed: () {
              context.read<DocumentProvider>().uploadDocument(
                    file,
                    description: descriptionController.text.trim().isEmpty
                        ? null
                        : descriptionController.text.trim(),
                  );
              Navigator.pop(context);
            },
            child: const Text('Uploader'),
          ),
        ],
      ),
    );
  }

  void _confirmDeleteFolder(FolderModel folder, DocumentProvider provider) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Confirmer la suppression'),
        content: Text(
            'Voulez-vous vraiment supprimer le dossier "${folder.name}" et tout son contenu?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Annuler'),
          ),
          ElevatedButton(
            onPressed: () {
              provider.deleteFolder(folder.id);
              Navigator.pop(context);
            },
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('Supprimer'),
          ),
        ],
      ),
    );
  }

  void _confirmDeleteDocument(DocumentModel document, DocumentProvider provider) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Confirmer la suppression'),
        content: Text(
            'Voulez-vous vraiment supprimer le fichier "${document.originalFilename}"?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Annuler'),
          ),
          ElevatedButton(
            onPressed: () {
              provider.deleteDocument(document.id);
              Navigator.pop(context);
            },
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('Supprimer'),
          ),
        ],
      ),
    );
  }

  void _downloadDocument(DocumentModel document) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Téléchargement de ${document.originalFilename}...'),
      ),
    );
  }

  void _previewDocument(DocumentModel document) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Aperçu de ${document.originalFilename}'),
      ),
    );
  }

  void _showSearchDialog() {
    final searchController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Rechercher'),
        content: TextField(
          controller: searchController,
          decoration: const InputDecoration(
            labelText: 'Rechercher un fichier',
            border: OutlineInputBorder(),
            prefixIcon: Icon(Icons.search),
          ),
          autofocus: true,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Annuler'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
            },
            child: const Text('Rechercher'),
          ),
        ],
      ),
    );
  }
}