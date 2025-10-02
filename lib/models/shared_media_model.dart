class SharedMediaModel {
  final int id;
  final String originalFilename;
  final String storedFilename;
  final String filePath;
  final int fileSize;
  final String? mimeType;
  final String fileType;
  final String uploadedBy;
  final String uploaderName;
  final int? duration;
  final String? thumbnailPath;
  final DateTime createdAt;
  final int? messageId;
  final String? messageContent;

  SharedMediaModel({
    required this.id,
    required this.originalFilename,
    required this.storedFilename,
    required this.filePath,
    required this.fileSize,
    this.mimeType,
    required this.fileType,
    required this.uploadedBy,
    required this.uploaderName,
    this.duration,
    this.thumbnailPath,
    required this.createdAt,
    this.messageId,
    this.messageContent,
  });

  factory SharedMediaModel.fromJson(Map<String, dynamic> json) {
    return SharedMediaModel(
      id: json['id'],
      originalFilename: json['originalFilename'],
      storedFilename: json['storedFilename'],
      filePath: json['filePath'],
      fileSize: json['fileSize'],
      mimeType: json['mimeType'],
      fileType: json['fileType'],
      uploadedBy: json['uploadedBy'],
      uploaderName: json['uploaderName'],
      duration: json['duration'],
      thumbnailPath: json['thumbnailPath'],
      createdAt: DateTime.parse(json['createdAt']),
      messageId: json['messageId'],
      messageContent: json['messageContent'],
    );
  }

  String getFormattedSize() {
    if (fileSize < 1024) {
      return '$fileSize B';
    } else if (fileSize < 1024 * 1024) {
      return '${(fileSize / 1024).toStringAsFixed(1)} KB';
    } else if (fileSize < 1024 * 1024 * 1024) {
      return '${(fileSize / (1024 * 1024)).toStringAsFixed(1)} MB';
    } else {
      return '${(fileSize / (1024 * 1024 * 1024)).toStringAsFixed(1)} GB';
    }
  }

  bool get isImage => fileType == 'IMAGE';
  bool get isVideo => fileType == 'VIDEO';
  bool get isDocument => fileType == 'DOCUMENT';
  bool get isAudio => fileType == 'AUDIO';
}
