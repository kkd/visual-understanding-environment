package tufts.oki.repository.fedora;

public class Part
implements osid.repository.Part
{
    private java.util.Vector partVector = new java.util.Vector();
    private osid.repository.RecordStructure recordStructure = null;
    private osid.shared.Id id = null;
    private java.io.Serializable value = null;
    private osid.repository.PartStructure partStructure = null;

    protected Part(osid.shared.Id id
                 , osid.repository.RecordStructure recordStructure
                 , osid.repository.PartStructure partStructure
                 , java.io.Serializable value)
    throws osid.repository.RepositoryException
    {
        this.id = id;
        this.recordStructure = recordStructure;
        this.partStructure = partStructure;
        this.value = value;
    }

    public String getDisplayName()
    throws osid.repository.RepositoryException
    {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    public void updateDisplayName(String displayName)
    throws osid.repository.RepositoryException
    {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    public osid.shared.Id getId()
    throws osid.repository.RepositoryException
    {
        return this.id;
    }

    public osid.repository.Part createPart(osid.shared.Id partStructureId
                                         , java.io.Serializable value)
    throws osid.repository.RepositoryException
    {
        if ( (partStructureId == null) || (value == null) )
        {
            throw new osid.repository.RepositoryException(osid.repository.RepositoryException.NULL_ARGUMENT);
        }

        osid.repository.PartStructureIterator psi = recordStructure.getPartStructures();
        
        while (psi.hasNextPartStructure()) 
        {
            osid.repository.PartStructure partStructure = psi.nextPartStructure();
            try 
            {
                if (partStructureId.isEqual(partStructure.getId())) 
                {
                    osid.repository.Part part = new Part(partStructureId, recordStructure, partStructure, value);
                    partVector.addElement(part);
                    this.partStructure = partStructure;
                    return part;
                }
            } 
            catch (osid.OsidException oex) 
            {
                throw new osid.repository.RepositoryException(osid.repository.RepositoryException.OPERATION_FAILED);
            }
        }
        throw new osid.repository.RepositoryException(osid.repository.RepositoryException.UNKNOWN_ID);
    }

    public void deletePart(osid.shared.Id partId)
    throws osid.repository.RepositoryException
    {
        if (partId == null)
        {
            throw new osid.repository.RepositoryException(osid.repository.RepositoryException.NULL_ARGUMENT);
        }
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    public osid.repository.PartIterator getParts()
    throws osid.repository.RepositoryException
    {
        return new PartIterator(this.partVector);
    }

    public java.io.Serializable getValue()
    throws osid.repository.RepositoryException
    {
        return this.value;
    }

    public void updateValue(java.io.Serializable value)
    throws osid.repository.RepositoryException
    {
        if (value == null)
        {
            throw new osid.repository.RepositoryException(osid.repository.RepositoryException.NULL_ARGUMENT);
        }
        this.value = value;
    }

    public osid.repository.PartStructure getPartStructure()
    throws osid.repository.RepositoryException
    {
        return this.partStructure;
    }
/**
<p>MIT O.K.I&#46; SID Implementation License.
  <p>	<b>Copyright and license statement:</b>
  </p>  <p>	Copyright &copy; 2003 Massachusetts Institute of
	Technology &lt;or copyright holder&gt;
  </p>  <p>	This work is being provided by the copyright holder(s)
	subject to the terms of the O.K.I&#46; SID Implementation
	License. By obtaining, using and/or copying this Work,
	you agree that you have read, understand, and will comply
	with the O.K.I&#46; SID Implementation License. 
  </p>  <p>	THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
	KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
	THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
	PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
	MASSACHUSETTS INSTITUTE OF TECHNOLOGY, THE AUTHORS, OR
	COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
	OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
	OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
	THE WORK OR THE USE OR OTHER DEALINGS IN THE WORK.
  </p>  <p>	<b>O.K.I&#46; SID Implementation License</b>
  </p>  <p>	This work (the &ldquo;Work&rdquo;), including software,
	documents, or other items related to O.K.I&#46; SID
	implementations, is being provided by the copyright
	holder(s) subject to the terms of the O.K.I&#46; SID
	Implementation License. By obtaining, using and/or
	copying this Work, you agree that you have read,
	understand, and will comply with the following terms and
	conditions of the O.K.I&#46; SID Implementation License:
  </p>  <p>	Permission to use, copy, modify, and distribute this Work
	and its documentation, with or without modification, for
	any purpose and without fee or royalty is hereby granted,
	provided that you include the following on ALL copies of
	the Work or portions thereof, including modifications or
	derivatives, that you make:
  </p>  <ul>	<li>	  The full text of the O.K.I&#46; SID Implementation
	  License in a location viewable to users of the
	  redistributed or derivative work.
	</li>  </ul>  <ul>	<li>	  Any pre-existing intellectual property disclaimers,
	  notices, or terms and conditions. If none exist, a
	  short notice similar to the following should be used
	  within the body of any redistributed or derivative
	  Work: &ldquo;Copyright &copy; 2003 Massachusetts
	  Institute of Technology. All Rights Reserved.&rdquo;
	</li>  </ul>  <ul>	<li>	  Notice of any changes or modifications to the
	  O.K.I&#46; Work, including the date the changes were
	  made. Any modified software must be distributed in such
	  as manner as to avoid any confusion with the original
	  O.K.I&#46; Work.
	</li>  </ul>  <p>	THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
	KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
	THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
	PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
	MASSACHUSETTS INSTITUTE OF TECHNOLOGY, THE AUTHORS, OR
	COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
	OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
	OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
	THE WORK OR THE USE OR OTHER DEALINGS IN THE WORK.
  </p>  <p>	The name and trademarks of copyright holder(s) and/or
	O.K.I&#46; may NOT be used in advertising or publicity
	pertaining to the Work without specific, written prior
	permission. Title to copyright in the Work and any
	associated documentation will at all times remain with
	the copyright holders.
  </p>  <p>	The export of software employing encryption technology
	may require a specific license from the United States
	Government. It is the responsibility of any person or
	organization contemplating export to obtain such a
	license before exporting this Work.
  </p>
*/
}
