#define AppName "Toluja Print Agent"
#ifndef AppVersion
#define AppVersion "0.1.0-dev"
#endif
#ifndef ClientSlug
#define ClientSlug "cliente"
#endif
#ifndef StoreSlug
#define StoreSlug "loja"
#endif
#ifndef StageDir
#define StageDir "..\build\windows-stage"
#endif
#ifndef OutputDir
#define OutputDir "..\build\windows-installer"
#endif

[Setup]
AppId={{5E24C41F-D459-43C2-A089-305359DFAE7F}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher=Toluja
DefaultDirName={autopf}\Toluja\PrintAgent
DefaultGroupName=Toluja
DisableProgramGroupPage=yes
OutputDir={#OutputDir}
OutputBaseFilename=TolujaPrintAgent-{#ClientSlug}-{#StoreSlug}-Setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=admin
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
UninstallDisplayName={#AppName}
SetupLogging=yes

[Dirs]
Name: "{commonappdata}\Toluja\PrintAgent"; Permissions: users-readexec admins-full
Name: "{commonappdata}\Toluja\PrintAgent\logs"; Permissions: users-modify admins-full

[Files]
Source: "{#StageDir}\app\*"; DestDir: "{app}\app"; Flags: recursesubdirs ignoreversion
Source: "{#StageDir}\runtime\*"; DestDir: "{app}\runtime"; Flags: recursesubdirs ignoreversion
Source: "{#StageDir}\service\TolujaPrintAgentService.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#StageDir}\service\TolujaPrintAgentService.xml"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#StageDir}\config\config.json"; DestDir: "{commonappdata}\Toluja\PrintAgent"; DestName: "config.json"; Flags: ignoreversion

[Icons]
Name: "{group}\Status do Toluja Print Agent"; Filename: "{app}\runtime\bin\java.exe"; Parameters: "-jar ""{app}\app\toluja-print-agent.jar"" --config ""{commonappdata}\Toluja\PrintAgent\config.json"" --config-check"; WorkingDir: "{app}"
Name: "{group}\Listar impressoras"; Filename: "{app}\runtime\bin\java.exe"; Parameters: "-jar ""{app}\app\toluja-print-agent.jar"" --list-printers"; WorkingDir: "{app}"

[Run]
Filename: "{app}\TolujaPrintAgentService.exe"; Parameters: "stop"; Flags: runhidden waituntilterminated ignoreerrors; StatusMsg: "Parando servico anterior..."; Check: ServiceFileExists
Filename: "{app}\TolujaPrintAgentService.exe"; Parameters: "uninstall"; Flags: runhidden waituntilterminated ignoreerrors; StatusMsg: "Removendo servico anterior..."; Check: ServiceFileExists
Filename: "{app}\TolujaPrintAgentService.exe"; Parameters: "install"; Flags: runhidden waituntilterminated; StatusMsg: "Instalando servico Toluja Print Agent..."
Filename: "{app}\TolujaPrintAgentService.exe"; Parameters: "start"; Flags: runhidden waituntilterminated; StatusMsg: "Iniciando servico Toluja Print Agent..."

[UninstallRun]
Filename: "{app}\TolujaPrintAgentService.exe"; Parameters: "stop"; Flags: runhidden waituntilterminated ignoreerrors
Filename: "{app}\TolujaPrintAgentService.exe"; Parameters: "uninstall"; Flags: runhidden waituntilterminated ignoreerrors

[Code]
function ServiceFileExists(): Boolean;
begin
  Result := FileExists(ExpandConstant('{app}\TolujaPrintAgentService.exe'));
end;
