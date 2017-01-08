/*
 * Copyright (C) 2016  John-Paul Gignac
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gignac.jp.pathological;
import android.graphics.*;
import android.content.*;
import java.io.*;

class Preview
{
    private static final float scale = 0.3f;
    public static final int width = Math.round(scale *
            (Board.horiz_tiles * Tile.tile_size + Marble.marble_size));
    public static final int height = Math.round(scale *
            (Board.vert_tiles * Tile.tile_size + Marble.marble_size));
    private static final int rows = 3;
    private static final int cols = 2;
    private static final int supersample = 2;
    private static final Rect dest = new Rect();
    private static BitmapBlitter b = null;
    private static final Paint paint = new Paint();

    public static void blit( Blitter d, int level,
                             int x, int y, int w, int h)
    {
        int segment = level / (rows*cols);
        int relLevel = level % (rows*cols);
        d.blit( 0x200000000L+segment,
            (width+1)*(relLevel/rows), (height+1)*(relLevel%rows),
            width, height, x, y, w, h);
    }

    private static void render( Canvas c,
        GameResources gr, SpriteCache s, int level)
    {
        int relLevel = level % (rows*cols);

        if(b == null) b = new BitmapBlitter(s,
            width*supersample, height*supersample);
        new Board(gr,s,level,null).paint(b);

        dest.left = (width+1)*(relLevel/rows);
        dest.top = (height+1)*(relLevel%rows);
        dest.right = dest.left + width;
        dest.bottom = dest.top + height;

        paint.setFilterBitmap(true);
        c.drawBitmap( b.getDest(), null, dest, paint);
    }

    public static void cache( Context c, SpriteCache s,
        GameResources gr, int nUnlocked)
    {
        for( int i=(nUnlocked-1)/(rows*cols); i >= 0; --i)
            cacheSegment(c,s,gr,i);
    }

    private static void cacheSegment(
        Context c, SpriteCache s, GameResources gr, int segment)
    {
        long uniq = 0x200000000L+segment;
        InputStream in = null;
        OutputStream out = null;
        Bitmap preview = s.getBitmap(uniq);
        if( preview != null) return;
        try {
            try {
                String name = "preview-s"+rows+"x"+cols+"-"+segment+".png";

                // Is the preview already cached?
                try {
                    in = c.openFileInput(name);
                    preview = BitmapFactory.decodeStream(in);
                } catch(FileNotFoundException e) {
                    int rw = (width+1) * cols - 1;
                    int rh = (height+1) * rows - 1;
                    preview = Bitmap.createBitmap(
                        rw, rh, Bitmap.Config.ARGB_8888);
                    Canvas cv = new Canvas(preview);
                    for( int j=0; j < cols; ++j) {
                        for( int i=0; i < rows; ++i) {
                            int level = (segment*cols+j)*rows+i;
                            if(level < gr.numlevels) render( cv, gr, s, level);
                        }
                    }

                    // Cache the image
                    out = c.openFileOutput(name, Context.MODE_PRIVATE);
                    preview.compress(Bitmap.CompressFormat.PNG, 90, out);
                }

                s.cache(uniq, preview);
            } finally {
                try {
                    if( in != null) in.close();
                } finally {
                    if( out != null) out.close();
                }
            }
        } catch( IOException e) {
            // Ignore
        }
    }

    public static void clearCache(Context c) {
        for( File file : c.getFilesDir().listFiles()) {
            if( file.getName().startsWith("preview-")) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }
}
