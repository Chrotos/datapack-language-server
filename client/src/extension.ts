import * as path from "path";
import * as net from 'net';
import fs = require('fs');
import child_process = require('child_process');
import { workspace, ExtensionContext, window, commands, Uri, MessageItem, StatusBarAlignment, ProgressLocation } from "vscode";

import {
  LanguageClient,
  LanguageClientOptions,
  StreamInfo,
} from "vscode-languageclient/node";

import * as selectVersion from "./selectVersion";
import * as updateVersions from "./updateVersions";

let client: LanguageClient;
let serverProcess: child_process.ChildProcess;
let netServer: net.Server;

export function activate(context: ExtensionContext) {
  const outputChannel = window.createOutputChannel("Datapack Language Server");
  
  const statusBarItem = window.createStatusBarItem(StatusBarAlignment.Right, 100);
  statusBarItem.command = 'datapack-language-server.select-version'
  context.subscriptions.push(statusBarItem);

  function updateStatusBar() {
    statusBarItem.text = `Datapack Language Server: ${context.workspaceState.get<string>('selectedVersion') || 'No version selected'}`;
    statusBarItem.show();
  }

  async function updateLanguageServer() {
    updateStatusBar();

    const selectedVersion = context.workspaceState.get<string>('selectedVersion');
    const selectedFullVersion = context.workspaceState.get<string>('selectedFullVersion');
  
    if (client) {
      if (client.isRunning()) {
        await client.stop();
      }
      client = null;
    }

    if (netServer) {
      if (netServer.listening) {
        netServer.close();
      }
      netServer = null;
    }

    if (serverProcess) {
      serverProcess.kill(2);
      serverProcess = null;
    }

    if (selectedVersion && selectedFullVersion) {
      // Options to control the language client
      const clientOptions: LanguageClientOptions = {
        // Register the server for all documents by default
        documentSelector: [{ scheme: "file", language: "mcfunction" }],
        outputChannel,
        //synchronize: {
          // Notify the server about file changes to '.clientrc files contained in the workspace
          //fileEvents: workspace.createFileSystemWatcher("**/.clientrc"),
        //},
      };
    
      function createServer(): Promise<StreamInfo> {
        // TODO get the jar file path from the user
        const versionDir = Uri.joinPath(context.globalStorageUri, 'versions', selectedVersion, selectedFullVersion)
        const versionJar = Uri.joinPath(versionDir, `${selectedFullVersion}.jar`)
        
        if (! fs.existsSync(path.resolve(versionDir.fsPath, 'plugins', 'server-1.0-SNAPSHOT-all.jar'))) {
          if (! fs.existsSync(path.resolve(versionDir.fsPath, 'plugins'))) {
            fs.mkdirSync(path.resolve(versionDir.fsPath, 'plugins'));
          }
          fs.copyFileSync(context.asAbsolutePath(path.join('server', 'build', 'libs', 'server-1.0-SNAPSHOT-all.jar')), path.resolve(versionDir.fsPath, 'plugins', 'server-1.0-SNAPSHOT-all.jar'));
        }
    
        return new Promise((resolve, reject) => {  
          netServer = net.createServer((socket) => {
            resolve({
              reader: socket,
              writer: socket
            });
          });
    
          netServer.listen(8123, () => {
            if (! serverProcess) {
              let args = [
                '-Xms500M',
                '-Xmx500M',
                '-Dcom.mojang.eula.agree=true',
                '-jar',
                versionJar.fsPath,
                '--nogui',
              ]
      
              const javaExecutablePath = findJavaExecutable('java');
      
              const serverProcess = child_process.spawn(javaExecutablePath, args, {
                cwd: versionDir.fsPath
              });
      
              serverProcess.stdout.on('data', (data) => {
                outputChannel.append(data.toString());
              });
            }
          })
        });
      }

      // Create the language client and start the client.
      client = new LanguageClient(
        "datapack-language-server language-server-id",
        "datapack-language-server language server name",
        createServer,
        clientOptions
      );
    
      // Start the client. This will also launch the server
      window.withProgress({
        location: ProgressLocation.Notification,
        cancellable: false,
        title: "Starting Server",
      }, async (progress) => {
        await client.start();
      });
    } else {
      const selectVersionItem = {
        title: "Select version",
        command: "datapack-language-server.select-version"
      }
  
      window.showInformationMessage("No version selected", selectVersionItem).then((item) => commands.executeCommand(item.command));
    }
  }

  updateLanguageServer();

  context.subscriptions.push(commands.registerCommand('datapack-language-server.select-version', selectVersion.executeCommand(context, updateLanguageServer)));
  context.subscriptions.push(commands.registerCommand('datapack-language-server.update-available-versions', updateVersions.executeCommand(context)));
}

export function deactivate(): Thenable<void> | undefined {
  if (!client) {
    return undefined;
  }

  serverProcess.kill();
  return client.stop();
}

// MIT Licensed code from: https://github.com/georgewfraser/vscode-javac
function findJavaExecutable(binname: string) {
	binname = correctBinname(binname);

	// First search each JAVA_HOME bin folder
	if (process.env['JAVA_HOME']) {
		let workspaces = process.env['JAVA_HOME'].split(path.delimiter);
		for (let i = 0; i < workspaces.length; i++) {
			let binpath = path.join(workspaces[i], 'bin', binname);
			if (fs.existsSync(binpath)) {
				return binpath;
			}
		}
	}

	// Then search PATH parts
	if (process.env['PATH']) {
		let pathparts = process.env['PATH'].split(path.delimiter);
		for (let i = 0; i < pathparts.length; i++) {
			let binpath = path.join(pathparts[i], binname);
			if (fs.existsSync(binpath)) {
				return binpath;
			}
		}
	}

	// Else return the binary name directly (this will likely always fail downstream) 
	return null;
}

function correctBinname(binname: string) {
	if (process.platform === 'win32')
		return binname + '.exe';
	else
		return binname;
}
