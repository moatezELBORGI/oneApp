import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/notification_provider.dart';
import '../../utils/app_theme.dart';

class FilesScreen extends StatefulWidget {
  const FilesScreen({super.key});

  @override
  State<FilesScreen> createState() => _FilesScreenState();
}

class _FilesScreenState extends State<FilesScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      appBar: AppBar(
        title: const Text('Mes Fichiers'),
        backgroundColor: Colors.white,
        elevation: 0,
        bottom: TabBar(
          controller: _tabController,
          labelColor: AppTheme.primaryColor,
          unselectedLabelColor: AppTheme.textSecondary,
          indicatorColor: AppTheme.primaryColor,
          tabs: const [
            Tab(text: 'Récents'),
            Tab(text: 'Images'),
            Tab(text: 'Documents'),
          ],
        ),
        actions: [
          Consumer<NotificationProvider>(
            builder: (context, notificationProvider, child) {
              return IconButton(
                onPressed: () {
                  notificationProvider.clearNewFiles();
                },
                icon: Stack(
                  children: [
                    const Icon(Icons.notifications_outlined),
                    if (notificationProvider.newFiles > 0)
                      Positioned(
                        right: 0,
                        top: 0,
                        child: Container(
                          padding: const EdgeInsets.all(2),
                          decoration: BoxDecoration(
                            color: AppTheme.errorColor,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          constraints: const BoxConstraints(
                            minWidth: 16,
                            minHeight: 16,
                          ),
                          child: Text(
                            '${notificationProvider.newFiles}',
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 10,
                              fontWeight: FontWeight.bold,
                            ),
                            textAlign: TextAlign.center,
                          ),
                        ),
                      ),
                  ],
                ),
              );
            },
          ),
        ],
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _buildRecentFiles(),
          _buildImageFiles(),
          _buildDocumentFiles(),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          // Simuler l'ajout d'un nouveau fichier
          Provider.of<NotificationProvider>(context, listen: false).incrementNewFiles();
        },
        backgroundColor: AppTheme.primaryColor,
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildRecentFiles() {
    final recentFiles = [
      {
        'name': 'Règlement intérieur.pdf',
        'type': 'PDF',
        'size': '2.3 MB',
        'date': '2 heures',
        'icon': Icons.picture_as_pdf,
        'color': Colors.red,
      },
      {
        'name': 'Photo_immeuble.jpg',
        'type': 'Image',
        'size': '1.8 MB',
        'date': '1 jour',
        'icon': Icons.image,
        'color': Colors.blue,
      },
      {
        'name': 'Facture_eau.pdf',
        'type': 'PDF',
        'size': '856 KB',
        'date': '3 jours',
        'icon': Icons.picture_as_pdf,
        'color': Colors.red,
      },
      {
        'name': 'Planning_travaux.docx',
        'type': 'Document',
        'size': '1.2 MB',
        'date': '1 semaine',
        'icon': Icons.description,
        'color': Colors.orange,
      },
    ];

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: recentFiles.length,
      itemBuilder: (context, index) {
        final file = recentFiles[index];
        return _buildFileCard(file);
      },
    );
  }

  Widget _buildImageFiles() {
    final imageFiles = [
      {
        'name': 'Photo_immeuble.jpg',
        'type': 'Image',
        'size': '1.8 MB',
        'date': '1 jour',
        'icon': Icons.image,
        'color': Colors.blue,
      },
      {
        'name': 'Jardin_commun.png',
        'type': 'Image',
        'size': '3.2 MB',
        'date': '2 jours',
        'icon': Icons.image,
        'color': Colors.blue,
      },
    ];

    return GridView.builder(
      padding: const EdgeInsets.all(16),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        crossAxisSpacing: 16,
        mainAxisSpacing: 16,
        childAspectRatio: 0.8,
      ),
      itemCount: imageFiles.length,
      itemBuilder: (context, index) {
        final file = imageFiles[index];
        return _buildImageCard(file);
      },
    );
  }

  Widget _buildDocumentFiles() {
    final documentFiles = [
      {
        'name': 'Règlement intérieur.pdf',
        'type': 'PDF',
        'size': '2.3 MB',
        'date': '2 heures',
        'icon': Icons.picture_as_pdf,
        'color': Colors.red,
      },
      {
        'name': 'Facture_eau.pdf',
        'type': 'PDF',
        'size': '856 KB',
        'date': '3 jours',
        'icon': Icons.picture_as_pdf,
        'color': Colors.red,
      },
      {
        'name': 'Planning_travaux.docx',
        'type': 'Document',
        'size': '1.2 MB',
        'date': '1 semaine',
        'icon': Icons.description,
        'color': Colors.orange,
      },
    ];

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: documentFiles.length,
      itemBuilder: (context, index) {
        final file = documentFiles[index];
        return _buildFileCard(file);
      },
    );
  }

  Widget _buildFileCard(Map<String, dynamic> file) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: ListTile(
        leading: Container(
          width: 48,
          height: 48,
          decoration: BoxDecoration(
            color: (file['color'] as Color).withOpacity(0.1),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(
            file['icon'] as IconData,
            color: file['color'] as Color,
            size: 24,
          ),
        ),
        title: Text(
          file['name'] as String,
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
              '${file['type']} • ${file['size']}',
              style: TextStyle(
                color: Colors.grey[600],
                fontSize: 12,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              'Il y a ${file['date']}',
              style: TextStyle(
                color: Colors.grey[500],
                fontSize: 11,
              ),
            ),
          ],
        ),
        trailing: PopupMenuButton(
          itemBuilder: (context) => [
            const PopupMenuItem(
              value: 'open',
              child: Row(
                children: [
                  Icon(Icons.open_in_new, size: 18),
                  SizedBox(width: 8),
                  Text('Ouvrir'),
                ],
              ),
            ),
            const PopupMenuItem(
              value: 'share',
              child: Row(
                children: [
                  Icon(Icons.share, size: 18),
                  SizedBox(width: 8),
                  Text('Partager'),
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
            // Handle file actions
            switch (value) {
              case 'open':
                // Open file
                break;
              case 'share':
                // Share file
                break;
              case 'delete':
                // Delete file
                break;
            }
          },
        ),
        onTap: () {
          // Open file
        },
      ),
    );
  }

  Widget _buildImageCard(Map<String, dynamic> file) {
    return Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Expanded(
            child: Container(
              decoration: BoxDecoration(
                color: Colors.grey[200],
                borderRadius: const BorderRadius.vertical(
                  top: Radius.circular(12),
                ),
              ),
              child: Icon(
                Icons.image,
                size: 64,
                color: Colors.grey[400],
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  file['name'] as String,
                  style: const TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: 14,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 4),
                Text(
                  file['size'] as String,
                  style: TextStyle(
                    color: Colors.grey[600],
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}