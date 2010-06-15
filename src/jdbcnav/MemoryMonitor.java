///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2010	Thomas Okken
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

import java.lang.ref.*;
import javax.swing.*;

public class MemoryMonitor implements Runnable {
	private static final int RESERVE_BYTES = 5242880;

	public MemoryMonitor() {
		Thread th = new Thread(this);
		th.setDaemon(true);
		th.start();
	}

	public void run() {
		while (true) {
			byte[] bigArray = new byte[RESERVE_BYTES];
			ReferenceQueue<byte[]> rq = new ReferenceQueue<byte[]>();
			new SoftReference<byte[]>(bigArray, rq);
			bigArray = null;
			boolean cleared = false;
			do {
				try {
					rq.remove();
					cleared = true;
					Main.log(3, "*** Reserve cleared ***");
				} catch (InterruptedException e) {}
			} while (!cleared);
			Runtime rt = Runtime.getRuntime();
			rt.gc();
			long space = rt.freeMemory() + rt.maxMemory() - rt.totalMemory();
			if (space < 2 * RESERVE_BYTES) {
				Main.log(3, "*** Memory low ***");
				SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							JOptionPane.showInternalMessageDialog(
								Main.getDesktop(),
								"Memory is running low!\n"
								+ "Try closing some table or query windows.");
							}
						});
				// Now we wait until enough free memory becomes available again
				// so we can re-allocate the reserve array. We actually wait
				// until *twice* RESERVE_BYTES are available, partly for
				// hysteresis, partly to avoid the OutOfMemoryErrors that can
				// easily occur if we reclaim *all* the available space.
				do {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {}
					rt.gc();
					space = rt.freeMemory() + rt.maxMemory() - rt.totalMemory();
					Main.log(3, "space = " + space);
				} while (space < 2 * RESERVE_BYTES);
				Main.log(3, "*** Memory OK ***");
			}
		}
	}
}
