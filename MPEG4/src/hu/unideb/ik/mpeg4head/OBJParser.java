package hu.unideb.ik.mpeg4head;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class OBJParser {
	int numVertices = 0;
	int numFaces = 0;
	Context context;

	private static final String TAG = "OBJParser.java";
	private static AssetManager assetMgr;
	Vector<Short> faces = new Vector<Short>();
	Vector<Short> vtPointer = new Vector<Short>();
	Vector<Short> vnPointer = new Vector<Short>();
	Vector<Float> v = new Vector<Float>();
	Vector<Float> vn = new Vector<Float>();
	Vector<Float> vt = new Vector<Float>();
	Vector<TDModelPart> parts = new Vector<TDModelPart>();
	Vector<Material> materials = null;

	public OBJParser(Context ctx) {
		context = ctx;
		assetMgr = ctx.getAssets();
	}

	public Mesh parseOBJ(String fileName) {
		BufferedReader reader = null;
		String line = null;
		Material m = null;
		Log.e(TAG, "parseOBJ RUNNING");
		try {
			/* Az asset mappából az .obj fájl betöltése. */
			reader = new BufferedReader(new InputStreamReader(
					assetMgr.open(fileName)));
		} catch (IOException e) {
		}
		/*Feldolgozzuk az egész .obj fájlt*/
		try { 
			/* Amíg van az új sorban tartalom ... */
			while ((line = reader.readLine()) != null) {
				Log.e("obj", line);
				/* Ez a sor felület pontjainak indexeit tartalmazza, aszerint dolgozzuk fel. */
				if (line.startsWith("f")) 			readFLine(line);
				/* Ez a sor normálvektorokat tartalmaz, aszerint dolgozzuk fel. */
				else if (line.startsWith("vn")) 	readVNLine(line); 
				/* Ez a sor textúra koordinátákat tartalmaz, aszerint dolgozzuk fel. */
				else if (line.startsWith("vt"))		readVTLine(line);
				/* Ez a sor vertex koordinátákat tartalmaz, aszerint dolgozzuk fel. */
				else if (line.startsWith("v"))		readVLine(line);
				/* Ez a sor material információkat tartalmaz, aszerint dolgozzuk fel. */
				else if (line.startsWith("usemtl")) {
					try {						
						/* Ha ez nem egy új csoport első sora, akkor ... */
						if (faces.size() != 0) {
							/* ... az eddig beolvasott adatokból létrehozunk egy részmodellt. */
							TDModelPart model = new TDModelPart(faces,
									vtPointer, vnPointer, m, vn);
							parts.add(model);
						}
						/* Kiolvassuk a fájl nevét. */
						String mtlName = line.split("[ ]+", 2)[1];
						/* A már beolvasott material-ok között megnézzük, nincs-e már ott ez is. */
						for (int i = 0; i < materials.size(); i++) {
							m = materials.get(i);
							if (m.getName().equals(mtlName)) {
								break;
							}
							/* Ha még nincs beolvasva, akkor kinullozzuk. */
							m = null;
						}
						faces = new Vector<Short>();
						vtPointer = new Vector<Short>();
						vnPointer = new Vector<Short>();
					} catch (Exception e) {
						// TODO: handle exception
					}
				} else if (line.startsWith("mtllib")) {
					materials = MTLParser.loadMTL(context,
							line.split("[ ]+")[1]);
					for (int i = 0; i < materials.size(); i++) {
						Material mat = materials.get(i);
						Log.v("materials", mat.toString());
					}
				}
			}
		} catch (IOException e) {
			System.out.println("wtf...");
		}
		if (faces != null) {// if not this is not the start of the first group
			TDModelPart model = new TDModelPart(faces, vtPointer, vnPointer, m,
					vn);
			parts.add(model);
		}
		Mesh mesh = new Mesh(context, v, vn, vt, parts); //Tibi's magic! Erre fogom cserélni az előző sort!
		mesh.buildBuffers(); //Tibi's magic! Erre fogom cserélni az előző sort!
		Log.d(TAG, mesh.toString());
		
		//return t;
		return mesh;
	}

	private void readVLine(String line) {
		String[] tokens = line.split("[ ]+"); // split the line at the spaces
		int c = tokens.length;
		for (int i = 1; i < c; i++) { // add the vertex to the vertex array
			v.add(Float.valueOf(tokens[i]));
		}
	}

	private void readVNLine(String line) {
		String[] tokens = line.split("[ ]+"); // split the line at the spaces
		int c = tokens.length;
		for (int i = 1; i < c; i++) { // add the vertex to the vertex array
			vn.add(Float.valueOf(tokens[i]));
		}
	}

	private void readVTLine(String line) {
		String[] tokens = line.split("[ ]+"); // split the line at the spaces
		int c = tokens.length;
		for (int i = 1; i < c; i++) { // add the vertex to the vertex array
			vt.add(Float.valueOf(tokens[i]));
		}
	}

	private void readFLine(String line) {
		String[] tokens = line.split("[ ]+");
		int c = tokens.length;

		if (tokens[1].matches("[0-9]+")) {// f: v
			if (c == 4) {// 3 faces
				for (int i = 1; i < c; i++) {
					Short s = Short.valueOf(tokens[i]);
					s--;
					faces.add(s);
				}
			} 
		}
		if (tokens[1].matches("[0-9]+/[0-9]+")) {// if: v/vt
			if (c == 4) {// 3 faces
				for (int i = 1; i < c; i++) {
					Short s = Short.valueOf(tokens[i].split("/")[0]);
					s--;
					faces.add(s);
					s = Short.valueOf(tokens[i].split("/")[1]);
					s--;
					vtPointer.add(s);
				}
			}
		}
		if (tokens[1].matches("[0-9]+//[0-9]+")) {// f: v//vn
			if (c == 4) {// 3 faces
				for (int i = 1; i < c; i++) {
					Short s = Short.valueOf(tokens[i].split("//")[0]);
					s--;
					faces.add(s);
					s = Short.valueOf(tokens[i].split("//")[1]);
					s--;
					vnPointer.add(s);
				}
			}
		}
		if (tokens[1].matches("[0-9]+/[0-9]+/[0-9]+")) {// f: v/vt/vn

			if (c == 4) {// 3 faces
				for (int i = 1; i < c; i++) {
					Short s = Short.valueOf(tokens[i].split("/")[0]);
					s--;
					faces.add(s);
					s = Short.valueOf(tokens[i].split("/")[1]);
					s--;
					vtPointer.add(s);
					s = Short.valueOf(tokens[i].split("/")[2]);
					s--;
					vnPointer.add(s);
				}
			} 
		}
	}

}
