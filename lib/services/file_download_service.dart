import 'dart:io';
import 'package:dio/dio.dart';
import 'package:path_provider/path_provider.dart';
import 'package:flutter/material.dart';

class FileDownloadService {
  static final FileDownloadService _instance = FileDownloadService._internal();
  factory FileDownloadService() => _instance;
  FileDownloadService._internal();

  final Dio _dio = Dio();

  Future<String?> downloadFile(String url, String fileName) async {
    try {
      // Obtenir le répertoire de téléchargement
      final directory = await getApplicationDocumentsDirectory();
      final filePath = '${directory.path}/$fileName';

      // Télécharger le fichier
      await _dio.download(
        url,
        filePath,
        onReceiveProgress: (received, total) {
          if (total != -1) {
            final progress = (received / total * 100).toStringAsFixed(0);
            print('Téléchargement: $progress%');
          }
        },
      );

      return filePath;
    } catch (e) {
      print('Erreur lors du téléchargement: $e');
      return null;
    }
  }

  Future<void> downloadAndSaveFile(
    BuildContext context,
    String url,
    String fileName,
  ) async {
    try {
      // Afficher un indicateur de chargement
      showDialog(
        context: context,
        barrierDismissible: false,
        builder: (context) => const AlertDialog(
          content: Row(
            children: [
              CircularProgressIndicator(),
              SizedBox(width: 16),
              Text('Téléchargement en cours...'),
            ],
          ),
        ),
      );

      final filePath = await downloadFile(url, fileName);

      // Fermer l'indicateur de chargement
      if (context.mounted) Navigator.of(context).pop();

      if (filePath != null) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Fichier téléchargé: $fileName'),
              backgroundColor: Colors.green,
              action: SnackBarAction(
                label: 'Ouvrir',
                onPressed: () {
                  // TODO: Ouvrir le fichier avec l'application par défaut
                },
              ),
            ),
          );
        }
      } else {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Erreur lors du téléchargement'),
              backgroundColor: Colors.red,
            ),
          );
        }
      }
    } catch (e) {
      // Fermer l'indicateur de chargement en cas d'erreur
      if (context.mounted) Navigator.of(context).pop();
      
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Erreur: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }
}