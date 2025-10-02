class FolderModel {
  final int id;
  final String name;
  final String folderPath;
  final int? parentFolderId;
  final int apartmentId;
  final String createdBy;
  final DateTime createdAt;
  final int subFolderCount;
  final int documentCount;

  FolderModel({
    required this.id,
    required this.name,
    required this.folderPath,
    this.parentFolderId,
    required this.apartmentId,
    required this.createdBy,
    required this.createdAt,
    this.subFolderCount = 0,
    this.documentCount = 0,
  });

  factory FolderModel.fromJson(Map<String, dynamic> json) {
    return FolderModel(
      id: json['id'],
      name: json['name'],
      folderPath: json['folderPath'],
      parentFolderId: json['parentFolderId'],
      apartmentId: json['apartmentId'],
      createdBy: json['createdBy'],
      createdAt: DateTime.parse(json['createdAt']),
      subFolderCount: json['subFolderCount'] ?? 0,
      documentCount: json['documentCount'] ?? 0,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'folderPath': folderPath,
      'parentFolderId': parentFolderId,
      'apartmentId': apartmentId,
      'createdBy': createdBy,
      'createdAt': createdAt.toIso8601String(),
      'subFolderCount': subFolderCount,
      'documentCount': documentCount,
    };
  }
}
