class FolderModel {
  final int id;
  final String name;
  final String folderPath;
  final int? parentFolderId;
  final String? apartmentId;
  final String? buildingId;
  final String createdBy;
  final bool isShared;
  final DateTime createdAt;
  final int subFolderCount;
  final int documentCount;

  FolderModel({
    required this.id,
    required this.name,
    required this.folderPath,
    this.parentFolderId,
    this.apartmentId,
    this.buildingId,
    required this.createdBy,
    this.isShared = false,
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
      buildingId: json['buildingId'],
      createdBy: json['createdBy'],
      isShared: json['isShared'] ?? false,
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
      'buildingId': buildingId,
      'createdBy': createdBy,
      'isShared': isShared,
      'createdAt': createdAt.toIso8601String(),
      'subFolderCount': subFolderCount,
      'documentCount': documentCount,
    };
  }
}
