/*
 * MuffinManager.java
 *
 * Created on January 30, 2001, 12:14 AM
 
 * Copyright © 2012 jbundle.org. All rights reserved.
 */
package org.jbundle.util.muffinmanager;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.jnlp.BasicService;
import javax.jnlp.ClipboardService;
import javax.jnlp.FileContents;
import javax.jnlp.FileOpenService;
import javax.jnlp.PersistenceService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

/** 
 * MuffinManager - This code handles the muffins for the java web start program.
 * @author  Administrator
 * @version 1.0.0
 */
public class MuffinManager extends Object
{
    /**
     * The basic jnlp services.
     */
    protected BasicService m_bs = null;
    /**
     * The jnlp persistence service.
     */
    protected PersistenceService m_ps = null;
    /**
     * The codebase to prefix muffins.
     */
    protected String m_strCodeBase = null;
    /**
     * The default encoding for muffins.
     */
    public static final String ENCODING = "UTF-8";

    /**
     * Creates new MuffinManager.
     */
    public MuffinManager()
    {
        super();
    }
    /**
     * Creates new MuffinManager .
     * @param applet The parent object (ignored).
     */
    public MuffinManager(Object applet)
    {
        this();
        this.init(applet);
    }
    /**
     * Creates new MuffinManager.
     * @param applet The parent object (ignored).
     */
    public void init(Object applet)
    {
        try {
            m_ps = (PersistenceService)ServiceManager.lookup("javax.jnlp.PersistenceService");
            m_bs = (BasicService)ServiceManager.lookup("javax.jnlp.BasicService");
            m_strCodeBase = m_bs.getCodeBase().toString();
        } catch (UnavailableServiceException e) {
            m_ps = null;
            m_bs = null;
        }
    }
    /**
     * Is the muffin manager service available?
     * @return true if available.
     */
    public boolean isServiceAvailable()
    {
        return (m_ps != null);
    }
    /**
     * Get the current value for this muffin.
     * @param strParam The key for this muffin parameter.
     * @return The value for this muffin (null if none).
     */
    public String getMuffin(String strParam)
    {
        try   {
            URL url = new URL(m_strCodeBase + strParam);
            FileContents fc = m_ps.get(url);
            if (fc == null)
                return null;
            // read in the contents of a muffin
            byte[] buf = new byte[(int)fc.getLength()];
            InputStream is = fc.getInputStream();
            int pos = 0;
            while((pos = is.read(buf, pos, buf.length - pos)) > 0) {
                // just loop
            }
            is.close();
            String strValue = new String(buf, ENCODING);
            return strValue;
        } catch (Exception ex)  {
            // Return null for any exception
        }
        return null;
    }
    /**
     * Set the current value for this muffin.
     * @param strParam The key for this muffin parameter.
     * @param strValue The value for this muffin.
     */
    public void setMuffin(String strParam, String strValue)
    {
        FileContents fc = null;
        URL url = null;
        try   {
            url = new URL(m_strCodeBase + strParam);
        } catch (Exception ex)  {
            return;
        }
        try   {
            fc = m_ps.get(url);
            fc.getMaxLength(); // This will throw an exception if there is no muffin yet.
        } catch (Exception ex)  {
            fc = null;
        }
        try   {
            if (fc == null)
            {
                m_ps.create(url, 100);
                fc = m_ps.get(url);
            }          // don't append
            if (strValue != null)
            {
                OutputStream os = fc.getOutputStream(false);
                byte[] buf = strValue.getBytes(ENCODING);
                os.write(buf);
                os.close();
                m_ps.setTag(url, PersistenceService.DIRTY);
            }
            else
                m_ps.delete(url);
        } catch (Exception ex)  {
            ex.printStackTrace(); // Return null for any exception
        }
    }
    /**
     * Get the Java WebStart codebase.
     * @return The codebase.
     */
    public URL getCodeBase()
    {
        if (m_bs != null)
            return m_bs.getCodeBase();
        return null;
    }
    /**
     * Display this URL in a web browser.
     * @param url The URL to display.
     * @return True if successfully displayed.
     */
    public boolean showTheDocument(URL url)
    {
        if (m_bs != null)
            return m_bs.showDocument(url);
        return false;
    }
    private ClipboardService cs = null;
    
    public static final int CLIPBOARD_ENABLED = 1 << 0;
    public static final int CLIPBOARD_DISABLED = 1 << 1;

    protected int clipboardReadStatus = 0;
    protected int clipboardWriteStatus = 0;

    /**
     * Get data from the system clipboard.
     * @return
     */
    public Transferable getClipboardContents()
    {
        if ((clipboardReadStatus & CLIPBOARD_DISABLED) == CLIPBOARD_DISABLED)
            return null;    // Rejected it last time, don't ask again
        clipboardReadStatus = CLIPBOARD_DISABLED;
        if (cs == null)
        {
            try {
                cs = (ClipboardService)ServiceManager.lookup("javax.jnlp.ClipboardService"); 
            } catch (UnavailableServiceException e) { 
                cs = null;
            }
        }

        if (cs != null) { 
            // get the contents of the system clipboard and print them 
            Transferable tr = cs.getContents(); 
            if (tr != null)
                clipboardReadStatus = CLIPBOARD_ENABLED;
            return tr;
        }
        return null;
    }
    /**
     * Set the global clipboard contents.
     * @param data
     */
    public boolean setClipboardContents(Transferable data)
    {
        if (data == null)
            return false;
        if ((clipboardWriteStatus & CLIPBOARD_DISABLED) == CLIPBOARD_DISABLED)
            return false;    // Rejected it last time, don't ask again
        clipboardWriteStatus = CLIPBOARD_ENABLED;
        if (cs == null)
        {
            try { 
                cs = (ClipboardService)ServiceManager.lookup("javax.jnlp.ClipboardService"); 
            } catch (UnavailableServiceException e) { 
                cs = null; 
            }
        }

        if (cs != null)
        { // set the system clipboard contents to a string selection 
            try { 
                cs.setContents(data);
                clipboardWriteStatus = CLIPBOARD_ENABLED;
                return true;
            } catch (Exception e) {
                e.printStackTrace(); 
            } 
        }
        return false;
    }
    
    /**
     * Replace the clipboard cut/cut/paste command with this command.
     * @param component
     * @param actionName
     * @return
     */
    public boolean replaceClipboardAction(JComponent component, String actionName)
    {
        Action action = (Action)component.getActionMap().get(actionName);
        if (action != null)
            component.getActionMap().put(actionName, new LinkedClipboardAction(actionName, action));
        return (action != null);
        
    }
    /**
     * Do cut/copy/paste from system clipboard, or if not possible, do it from local clipboard.
     * @author don
     *
     */
    class LinkedClipboardAction extends TextAction   // Action?
    {
        private static final long serialVersionUID = 1L;
        Action linkedAction;
        public LinkedClipboardAction(String name, Action linkedAction)
        {
            super(name);
            this.linkedAction = linkedAction;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            String name = (String)this.getValue(Action.NAME);
            if (target != null) {
                {
                    if ("paste".equals(name))
                    {
                        Transferable contents = getClipboardContents();
                        if (contents != null) { 
                            try {
                                String s = (String)contents.getTransferData(DataFlavor.stringFlavor);
                                if (s != null)
                                {
                                    target.replaceSelection(s);
                                    return;
                                }
                            } catch (UnsupportedFlavorException e1) {
                                e1.printStackTrace();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                }
                
                linkedAction.actionPerformed(e);
                
                {
                    if (("cut".equals(name)) || ("copy".equals(name)))
                    {
                        String data = target.getSelectedText();
                        StringSelection ss = new StringSelection(data); 
                        if (ss != null)
                            setClipboardContents(ss);
                    }
                }
            }
        }
    }
    
    FileOpenService fos = null;;
    /**
     * Open this file.
     * @return
     */
    public InputStream openFileStream(String pathHint, String[] extensions)
    {
        if (fos == null)
        {
            try { 
                fos = (FileOpenService)ServiceManager.lookup("javax.jnlp.FileOpenService"); 
            } catch (UnavailableServiceException e) { 
                fos = null; 
            }
        }

        if (fos != null) { 
            try { 
                // ask user to select a file through this service 
                FileContents fc = fos.openFileDialog(pathHint, extensions);
                return fc.getInputStream();
            } catch (Exception e) { 
                e.printStackTrace(); 
            } 
        } 
        return null;
    }

}
