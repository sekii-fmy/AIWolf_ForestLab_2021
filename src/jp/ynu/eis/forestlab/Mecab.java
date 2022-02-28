package jp.ynu.eis.forestlab;

import net.moraleboost.mecab.Lattice;
import net.moraleboost.mecab.Node;
import net.moraleboost.mecab.impl.StandardTagger;

public class Mecab {
	StandardTagger tagger;
	Lattice lattice;
	String planeText;
	
	public Mecab() {
		// Taggerを構築。
		// 引数には、MeCabのcreateTagger()関数に与える引数を与える。
		tagger = new StandardTagger("");
		//Lattice（形態素解析に必要な実行時情報が格納されるオブジェクト）を構築
		lattice = tagger.createLattice();
	}
	
	public Node mecabParse(String text) {
		planeText = text;
		
		//解析対象文字列をセット
        lattice.setSentence(text);
        
        //tagger.parse()を呼び出して、文字列を形態素解析する。
        tagger.parse(lattice);
        Node node = lattice.bosNode();
        
        return node;
	}
	
	public void destroyLattice(Lattice lattice) {
		//latticeを破壊
        lattice.destroy();
	}
	
	public void destroyTagger(StandardTagger tagger) {
		//taggerを破壊
        tagger.destroy();
	}

}
