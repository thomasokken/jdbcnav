///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2023  Thomas Okken
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License, version 2,
// as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
///////////////////////////////////////////////////////////////////////////////

package jdbcnav;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;


public class Clipboard {
    private ArrayList<Listener> listeners;
    private java.awt.datatransfer.Clipboard sysClip;
    private static DataFlavor gridFlavor;
    static {
        try {
            gridFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
                        + "; class=\"[[Ljava.lang.Object;\"");
        } catch (ClassNotFoundException e) {
            // Won't happen.
        }
    }
    private static DataFlavor byteArrayFlavor;
    static {
        try {
            byteArrayFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
                        + "; class=\"[B\"");
        } catch (ClassNotFoundException e) {
            // Won't happen.
        }
    }

    private class GridSelection implements Transferable {
        private Object[][] grid;
        public GridSelection(Object[][] grid) {
            this.grid = grid;
        }
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { gridFlavor, DataFlavor.stringFlavor };
        }
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(gridFlavor) || flavor.equals(DataFlavor.stringFlavor);
        }
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (flavor.equals(gridFlavor))
                return grid;
            else if (flavor.equals(DataFlavor.stringFlavor))
                return grid.toString();
            else
                throw new UnsupportedFlavorException(flavor);
        }
    }

    private class ByteArraySelection implements Transferable {
        private byte[] bytes;
        public ByteArraySelection(byte[] bytes) {
            this.bytes = bytes;
        }
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { byteArrayFlavor, DataFlavor.stringFlavor };
        }
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(byteArrayFlavor) || flavor.equals(DataFlavor.stringFlavor);
        }
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (flavor.equals(byteArrayFlavor))
                return bytes;
            else if (flavor.equals(DataFlavor.stringFlavor))
                return bytesAsString();
            else
                throw new UnsupportedFlavorException(flavor);
        }
        private String bytesAsString() {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < bytes.length; i++) {
                byte b = bytes[i];
                buf.append("0123456789abcdef".charAt((b >> 4) & 15));
                buf.append("0123456789abcdef".charAt(b & 15));
            }
            return buf.toString();
        }
    }

    public Clipboard() {
        sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
        listeners = new ArrayList<Listener>();
    }

    public void put(Object data) {
        Transferable tr;
        if (data == null)
            tr = null;
        else if (data instanceof Object[][])
            tr = new GridSelection((Object[][]) data);
        else if (data instanceof byte[])
            tr = new ByteArraySelection((byte[]) data);
        else
            tr = new StringSelection(data.toString());
        try {
            sysClip.setContents(tr, null);
        } catch (IllegalStateException e) {}
        notifyListeners();
    }

    public Object get() {
        Transferable tr;
        try {
            tr = sysClip.getContents(null);
        } catch (IllegalStateException e) {
            return null;
        }
        try {
            if (tr.isDataFlavorSupported(gridFlavor))
                return tr.getTransferData(gridFlavor);
            else if (tr.isDataFlavorSupported(byteArrayFlavor))
                return tr.getTransferData(byteArrayFlavor);
            else
                return tr.getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public void refresh() {
        notifyListeners();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public interface Listener {
        void clipboardUpdated(Object data);
    }

    private void notifyListeners() {
        Object data = get();
        for (Listener listener : listeners)
            listener.clipboardUpdated(data);
    }
}
