package com.chat.serveur;

import com.chat.commun.net.Connexion;
import com.echecs.PartieEchecs;

import java.util.ListIterator;
import java.util.Vector;

/**
 * Cette classe �tend (h�rite) la classe abstraite Serveur et y ajoute le n�cessaire pour que le
 * serveur soit un serveur de chat.
 *
 * @author Abdelmoum�ne Toudeft (Abdelmoumene.Toudeft@etsmtl.ca)
 * @version 1.0
 * @since 2023-09-15
 */
public class ServeurChat extends Serveur {
    private final Vector<String> historique = new Vector<>();
    private final Vector<Invitation> invitations = new Vector<>();
    private final Vector<SalonPrive> salonsPrives = new Vector<>();
    /**
     * Cr�e un serveur de chat qui va �couter sur le port sp�cifi�.
     *
     * @param port int Port d'�coute du serveur
     */
    public ServeurChat(int port) {
        super(port);
    }

    @Override
    public synchronized boolean ajouter(Connexion connexion) {
        boolean ajoute;
        String hist = this.historique();
        if ("".equals(hist)) {
            connexion.envoyer("OK");
        }
        else {
            connexion.envoyer("HIST " + hist);
        }
        ajoute = super.ajouter(connexion);
        envoyerATousSauf("NEW "+connexion.getAlias(), connexion.getAlias());
        return ajoute;
    }

    @Override
    public synchronized boolean enlever(Connexion connexion) {
        super.enlever(connexion);
        String alias = connexion.getAlias();
        ListIterator<Invitation> it = invitations.listIterator();
        Invitation inv;
        while (it.hasNext()) {
            inv = it.next();
            if (alias.equals(inv.getInvite()) || alias.equals(inv.getHote()))
                    it.remove();
        }
        ListIterator<SalonPrive> it2 = salonsPrives.listIterator();
        SalonPrive sp;
        while (it2.hasNext()) {
            sp = it2.next();
            if (alias.equals(sp.getInvite()) || alias.equals(sp.getHote()))
                it2.remove();
        }
        return true;
    }

    /**
     * Retourne la liste des alias des connect�s au serveur dans une cha�ne de caract�res.
     *
     * @return String cha�ne de caract�res contenant la liste des alias des membres connect�s sous la
     * forme alias1:alias2:alias3 ...
     */
    public String list() {
        String s = "";
        for (Connexion cnx:connectes)
            s+=cnx.getAlias()+":";
        return s;
    }
    /**
     * Valide l'arriv�e d'un nouveau client sur le serveur. Cette red�finition
     * de la m�thode h�rit�e de Serveur v�rifie si le nouveau client a envoy�
     * un alias compos� uniquement des caract�res a-z, A-Z, 0-9, - et _.
     *
     * @param connexion Connexion la connexion repr�sentant le client
     * @return boolean true, si le client a valid� correctement son arriv�e, false, sinon
     */
    @Override
    protected boolean validerConnexion(Connexion connexion) {

        String texte = connexion.getAvailableText().trim();
        char c;
        int taille;
        boolean res = true;
        if ("".equals(texte)) {
            return false;
        }
        taille = texte.length();
        for (int i=0;i<taille;i++) {
            c = texte.charAt(i);
            if ((c<'a' || c>'z') && (c<'A' || c>'Z') && (c<'0' || c>'9')
                    && c!='_' && c!='-') {
                connexion.envoyer("WAIT_FOR alias MALFORMED");
                return false;
            }
        }
        for (Connexion cnx:connectes) {
            if (texte.equalsIgnoreCase(cnx.getAlias())) { //alias d�j� utilis�
                connexion.envoyer("WAIT_FOR alias USED");
                return false;
            }
        }
        connexion.setAlias(texte);
        return true;
    }
    public String historique() {
        String s = "";
        for (String str:historique)
            s+=str+"\n";
        return s;
    }
    public void ajouterHistorique(String msg) {
        this.historique.add(msg);
    }

    /**
     * Envoie un texte � tous les connect�s sauf celui qui a l'alias fourni.
     *
     * @param str String texte � envoyer.
     * @param aliasExpediteur String alias de la personne exclue de l'envoi.
     */
    public void envoyerATousSauf(String str, String aliasExpediteur) {
        for (Connexion cnx:connectes)
            if (!cnx.getAlias().equals(aliasExpediteur))
                cnx.envoyer(str);
    }
    public int traiterInvitation(String hote, String invite) {
        for (Invitation inv: invitations) {
            if (hote.equals(inv.getHote()) && invite.equals(inv.getInvite())) { //d�j� invit� par l'h�te
                return Invitation.DEJA_LA;
            }
            else if (hote.equals(inv.getInvite()) && invite.equals(inv.getHote())) { //accepter invitation
                return Invitation.ACCEPTEE;
            }
        }
        invitations.add(new Invitation(hote,invite));
        return Invitation.AJOUTEE;
    }
    public void ajouter(SalonPrive salonPrive) {
        salonsPrives.add(salonPrive);
    }
    public Connexion getConnexionParAlias(String alias) {
        for (Connexion cnx : connectes)
            if (alias.equals(cnx.getAlias()))
                return cnx;
        return null;
    }
    public String listInvitations(String alias) {
        String s = "";
        for (Invitation inv:invitations)
            if (alias.equals(inv.getInvite()))
                s+=inv.getHote()+":";
        return s;
    }
    public boolean supprimeInvitation(String hote, String invite) {
        for (Invitation inv: invitations) {
            if (hote.equals(inv.getHote()) && invite.equals(inv.getInvite())
                || hote.equals(inv.getInvite()) && invite.equals(inv.getHote())) { //supprimer invitation
                invitations.remove(inv);
                return true;
            }
        }
        return false;
    }
    public boolean supprimeSalonPrive(String hote, String invite) {
        /*
         Puisque nous avons red�finit equals() dans SalonPrive pour dire quand
         2 salons priv�s sont �gaux ou diff�rents, il suffit ici d'utiliser
         remove() du vecteur :
         */
        return salonsPrives.remove(new SalonPrive(hote,invite));
    }
    public boolean enPrive(String alias1, String alias2) {
        /*
         Puisque nous avons red�finit equals() dans SalonPrive pour dire quand
         2 salons priv�s sont �gaux ou diff�rents, il suffit ici d'utiliser
         contains() du vecteur :
         */
        return salonsPrives.contains(new SalonPrive(alias1,alias2));
    }
    public SalonPrive getSalonPrive(String aliasExpediteur, String aliasDestinataire) {
        /*
         Puisque nous avons red�finit equals() dans SalonPrive pour dire quand
         2 salons priv�s sont �gaux ou diff�rents, il suffit ici d'utiliser
         indexOf() du vecteur :
         */
        int i = this.salonsPrives.indexOf(new SalonPrive(aliasExpediteur,aliasDestinataire));
        if (i!=-1)
            return this.salonsPrives.elementAt(i);
        return null;
    }

    public PartieEchecs getPartieDe(String alias) {
        for (SalonPrive sp : salonsPrives)
            if ((alias.equals(sp.getHote()) || alias.equals(sp.getInvite())) && sp.getPartieEchecs()!=null)
                return sp.getPartieEchecs();
        return null;
    }

    public boolean supprimeInvitationEchecs(String hote, String aliasExpediteur) {
        SalonPrive sp = getSalonPrive(hote,aliasExpediteur);
        if (sp==null)
            return false;
        return sp.supprimeInvitationAuxEchecs();
    }
}