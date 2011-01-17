package ch.parren.jdepchk.classes;

import java.io.File;
import java.io.IOException;

public final class PathClassFileSet implements ClassFileSet {

	private final File baseDir;
	private final String baseDirPath;

	public PathClassFileSet(File baseDir) {
		this.baseDir = baseDir;
		this.baseDirPath = baseDir.getPath() + File.separator;
	}

	@Override public void accept(Visitor visitor) throws IOException {
		accept(visitor, this.baseDir, "");
	}

	private void accept(Visitor visitor, File dir, String packagePath) throws IOException {
		if (!visitor.visitPackage(packagePath))
			return;
		for (File file : dir.listFiles()) {
			final String name = file.getName();
			if (name.charAt(0) == '.')
				; // ignore hidden files
			else if (name.endsWith(".class"))
				accept(visitor, file);
			else if (file.isDirectory())
				accept(visitor, file, packagePath.isEmpty() ? name : packagePath + "/" + name);
		}
	}

	private void accept(Visitor visitor, File file) throws IOException {
		final DirectClassFileReader classFile = new DirectClassFileReader(this.baseDirPath, file);
		try {
			visitor.visitClassFile(classFile);
		} finally {
			classFile.close();
		}
	}

}
