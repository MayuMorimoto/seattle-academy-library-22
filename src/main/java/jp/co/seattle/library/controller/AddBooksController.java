package jp.co.seattle.library.controller;

import java.util.ArrayList;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jp.co.seattle.library.dto.BookDetailsInfo;
import jp.co.seattle.library.service.BooksService;
import jp.co.seattle.library.service.ThumbnailService;

/**
 * Handles requests for the application home page.
 */
@Controller // APIの入り口
public class AddBooksController {
	final static Logger logger = LoggerFactory.getLogger(AddBooksController.class);

	@Autowired
	private BooksService booksService;

	@Autowired
	private ThumbnailService thumbnailService;

	@RequestMapping(value = "/addBook", method = RequestMethod.GET) // value＝actionで指定したパラメータ
	// RequestParamでname属性を取得
	public String login(Model model) {
		return "addBook";
	}

	/**
	 * 書籍情報を登録する
	 * 
	 * @param locale    ロケール情報
	 * @param title     書籍名
	 * @param author    著者名
	 * @param publisher 出版社
	 * @param file      サムネイルファイル
	 * @param model     モデル
	 * @return 遷移先画面
	 */
	@Transactional
	@RequestMapping(value = "/insertBook", method = RequestMethod.POST, produces = "text/plain;charset=utf-8")
	public String insertBook(Locale locale, @RequestParam("title") String title, @RequestParam("author") String author,
			@RequestParam("publisher") String publisher, @RequestParam("thumbnail") MultipartFile file,
			@RequestParam("publishDate") String publishDate, @RequestParam("isbn") String isbn,
			@RequestParam("detail") String detail, Model model) {
		logger.info("Welcome insertBooks.java! The client locale is {}.", locale);

		// パラメータで受け取った書籍情報をDtoに格納する。
		BookDetailsInfo bookInfo = new BookDetailsInfo();
		bookInfo.setTitle(title);
		bookInfo.setAuthor(author);
		bookInfo.setPublisher(publisher);
		bookInfo.setPublishDate(publishDate);
		bookInfo.setIsbn(isbn);
		bookInfo.setDetail(detail);

		// クライアントのファイルシステムにある元のファイル名を設定する
		String thumbnail = file.getOriginalFilename();

		if (!file.isEmpty()) {
			try {
				// サムネイル画像をアップロード
				String fileName = thumbnailService.uploadThumbnail(thumbnail, file);
				// URLを取得
				String thumbnailUrl = thumbnailService.getURL(fileName);

				bookInfo.setThumbnailName(fileName);
				bookInfo.setThumbnailUrl(thumbnailUrl);

			} catch (Exception e) {

				// 異常終了時の処理
				logger.error("サムネイルアップロードでエラー発生", e);
				model.addAttribute("bookDetailsInfo", bookInfo);
				return "addBook";
			}
		}

		// 各入力項目のバリデーションチェック
		// チェック内容を格納する配列を作成
		ArrayList<String> errorList = new ArrayList<String>();
		// 必須入力チェック
		if (StringUtils.isEmpty(title) || StringUtils.isEmpty(author) || StringUtils.isEmpty(publisher)
				|| StringUtils.isEmpty(publishDate)) {
			errorList.add("必須項目が未入力です。");
		}
		// 出版日のチェック（半角数字8文字であればOK）
		if (!publishDate.matches("^\\d{8}$")) {
			errorList.add("出版日は半角数字のYYYYMMDD形式で入力してください。");
		}
		// ISBNの桁数チェック（半角数字10桁or13桁であればOK）
		if (!(isbn.matches("^\\d{10}$")) && !(isbn.matches("^\\d{13}$"))) {
			errorList.add("ISBNは10桁または13桁で入力してください。");
		}
		//1件以上バリデーションエラーが存在する場合、画面にエラーメッセージ表示
		if (!CollectionUtils.isEmpty(errorList)) {
			model.addAttribute("errorList", errorList);
			model.addAttribute("bookInfo", bookInfo);
			return "addBook";
		}

		// 書籍情報を新規登録する
		booksService.registBook(bookInfo);

		// 登録した書籍の詳細情報を表示するように実装
		model.addAttribute("bookDetailsInfo", booksService.getLatestBookId());
		// 詳細画面に遷移する
		return "details";
	}

}
