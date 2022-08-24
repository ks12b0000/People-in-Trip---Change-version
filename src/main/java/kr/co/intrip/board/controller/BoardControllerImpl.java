package kr.co.intrip.board.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import kr.co.intrip.board.dto.BoardDTO;
import kr.co.intrip.board.dto.CommentPagingDTO;
import kr.co.intrip.board.dto.Criteria;
import kr.co.intrip.board.dto.ImageDTO;
import kr.co.intrip.board.dto.PageMaker;
import kr.co.intrip.board.dto.SearchCriteria;
import kr.co.intrip.board.dto.boardCommentDTO;
import kr.co.intrip.board.service.BoardService;
import kr.co.intrip.login_signup.dto.MemberDTO;
import kr.co.intrip.tourist.dto.ApiDTO;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class BoardControllerImpl implements BoardController {

	@Autowired
	private BoardService boardService;
	
	@Autowired
	private BoardDTO boardDTO;
	
	private static String ARTICLE_IMAGE_REPO = "/Users/parkhyunjoon/Desktop/test/imageRepo";
	private static String ARTICLE_IMAGE_REPO1 = "/Users/parkhyunjoon/Desktop/test/imageRepo1";

	// 상세보기
	@Override
	@RequestMapping(value = "/board/community_detail.do", method = RequestMethod.GET)
	public ModelAndView viewdetail(@RequestParam(value = "post_num") int post_num, // 조회할 글 번호를 가져옴
			HttpServletRequest request, HttpServletResponse response, Model model,
			@ModelAttribute("commentpagingDTO") CommentPagingDTO commentpagingDTO) throws Exception {

		// 조회수 증가
		boardService.visitcount(post_num);
		String viewName = (String) request.getAttribute("viewName");

		Map<String, Object> boardMap = boardService.viewdetail(post_num); // 조회할 글 정보,이미지파일 정보를 articleMap에 설정
		ModelAndView mav = new ModelAndView();
		mav.setViewName(viewName);
		mav.addObject("boardMap", boardMap);

		int totalRowCount = boardService.getboardCommentTotalRowCount(commentpagingDTO);
		commentpagingDTO.setTotalRowCount(totalRowCount);
		commentpagingDTO.pageSetting();
		List<boardCommentDTO> replyList = boardService.boardreadReply(commentpagingDTO);
		model.addAttribute("replyList", replyList);

		return mav;
	}

	// 상세보기1
	@Override
	@RequestMapping(value = "/board/community_detail2.do", method = RequestMethod.GET)
	public ModelAndView viewdetail1(@RequestParam(value = "post_num") int post_num, // 조회할 글 번호를 가져옴
			HttpServletRequest request, HttpServletResponse response, Model model,
			@ModelAttribute("commentpagingDTO") CommentPagingDTO commentpagingDTO) throws Exception {

		// 조회수 증가
		boardService.visitcount1(post_num);
		String viewName = (String) request.getAttribute("viewName");

		Map<String, Object> boardMap = boardService.viewdetail1(post_num); // 조회할 글 정보,이미지파일 정보를 articleMap에 설정

		ModelAndView mav = new ModelAndView();
		mav.setViewName(viewName);
		mav.addObject("boardMap", boardMap);

		int totalRowCount = boardService.getboardCommentTotalRowCount2(commentpagingDTO);
		commentpagingDTO.setTotalRowCount(totalRowCount);
		commentpagingDTO.pageSetting();
		List<boardCommentDTO> replyList = boardService.boardreadReply2(commentpagingDTO);
		model.addAttribute("replyList", replyList);

		return mav;
	}

	// 글쓰기 페이지
	@GetMapping("/board/community_writeInfo.do")
	public String addnewboard() {
		return "board/community_writeInfo";
	}

	// 글쓰기 페이지1
	@GetMapping("board/community_writeWith.do")
	public String addnewboard1() {
		return "board/community_writeWith";
	}

	// 글쓰기
	@Override
	@RequestMapping(value = "/board/community_writeInfo.do", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity addNewArticle(MultipartHttpServletRequest multipartRequest, HttpServletResponse response)
			throws Exception {

		multipartRequest.setCharacterEncoding("utf-8");
		String imageFileName = null;

		// 글정보 저장하기 위한 Map 생성
		Map boardMap = new HashMap();
		Enumeration enun = multipartRequest.getParameterNames();
		// 새글쓰기창에서 전송된 글 정보를 Map에 key/value로 저장함
		while (enun.hasMoreElements()) {
			String name = (String) enun.nextElement();
			String value = multipartRequest.getParameter(name);
			boardMap.put(name, value);
		}

		// 로그인 시 세션에 저장된 회원정보에서 아이디(글쓴이)를 Map에 저장
		HttpSession session = multipartRequest.getSession();
		MemberDTO memberDTO = (MemberDTO) session.getAttribute("user");
		String id = memberDTO.getId();
		boardMap.put("id", id);

		// 업로드한 이미지 파일 이름을 가져옴
		List<String> fileList = upload(multipartRequest);

		List<ImageDTO> imageFileList = new ArrayList<>();
		if (fileList != null && fileList.size() != 0) {
			// 전송되는 이미지 정보를 ImageDTO 객체의 속성에 차례대로 저장한 후 imageFileList에 다시 저장함
			for (String fileName : fileList) {
				ImageDTO imageDTO = new ImageDTO();
				imageDTO.setImageFileName(fileName);
				imageFileList.add(imageDTO);
			}
			// imageFileList를 다시 boardMap에 저장함
			boardMap.put("imageFileList", imageFileList);
		}

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/html; charset=utf-8");

		String message;
		ResponseEntity resEnt = null;

		try {

			int post_num = boardService.insertBoard(boardMap);

			if (imageFileList != null && imageFileList.size() != 0) {
				// 첨부한 이미지들을 for문을 이용해 업로드함
				for (ImageDTO imageDTO : imageFileList) {

					imageFileName = imageDTO.getImageFileName();
					File srcFile = new File(ARTICLE_IMAGE_REPO + "//" + "temp" + "//" + imageFileName);
					File destFile = new File(ARTICLE_IMAGE_REPO + "//" + post_num);
					FileUtils.moveFileToDirectory(srcFile, destFile, true);
				}
			}

			message = "<script>";
			message += " alert('새글을 추가했습니다.');";
			message += " location.href='" + multipartRequest.getContextPath() + "/board/community-acco';";
			message += "</script>";

			// 새 글을 추가한 후 메시지를 전달함
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);

		} catch (Exception e) {
			if (imageFileList != null && imageFileList.size() != 0) {
				// 오류 발생시 temp폴더의 이미지들 모두 삭제
				for (ImageDTO imageDTO : imageFileList) {
					imageFileName = imageDTO.getImageFileName();
					File srcFile = new File(ARTICLE_IMAGE_REPO + "//" + "temp" + "//" + imageFileName);
					srcFile.delete();
				}
			}

			message = "<script>";
			message += " alert('빈칸없이 입력해 주세요!.');";
			message += " location.href='" + multipartRequest.getContextPath() + "/board/community-acco';";
			message += "</script>";
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);

			e.printStackTrace();
		}

		return resEnt;
	}

	// 글쓰기 1
	@Override
	@RequestMapping(value = "board/community_writeWith.do", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity addNewArticle1(MultipartHttpServletRequest multipartRequest, HttpServletResponse response)
			throws Exception {

		multipartRequest.setCharacterEncoding("utf-8");
		String imageFileName = null;

		// 글정보 저장하기 위한 Map 생성
		Map boardMap = new HashMap();
		Enumeration enun = multipartRequest.getParameterNames();
		// 새글쓰기창에서 전송된 글 정보를 Map에 key/value로 저장함
		while (enun.hasMoreElements()) {
			String name = (String) enun.nextElement();
			String value = multipartRequest.getParameter(name);
			boardMap.put(name, value);
		}

		// 로그인 시 세션에 저장된 회원정보에서 아이디(글쓴이)를 Map에 저장
		HttpSession session = multipartRequest.getSession();
		MemberDTO memberDTO = (MemberDTO) session.getAttribute("user");
		String id = memberDTO.getId();
		boardMap.put("id", id);

		// 업로드한 이미지 파일 이름을 가져옴
		List<String> fileList = upload1(multipartRequest);

		List<ImageDTO> imageFileList = new ArrayList<>();
		if (fileList != null && fileList.size() != 0) {
			// 전송되는 이미지 정보를 ImageDTO 객체의 속성에 차례대로 저장한 후 imageFileList에 다시 저장함
			for (String fileName : fileList) {
				ImageDTO imageDTO = new ImageDTO();
				imageDTO.setImageFileName(fileName);
				imageFileList.add(imageDTO);
			}
			// imageFileList를 다시 boardMap에 저장함
			boardMap.put("imageFileList", imageFileList);
		}

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/html; charset=utf-8");

		String message;
		ResponseEntity resEnt = null;

		try {

			int post_num = boardService.insertBoard1(boardMap);

			if (imageFileList != null && imageFileList.size() != 0) {
				// 첨부한 이미지들을 for문을 이용해 업로드함
				for (ImageDTO imageDTO : imageFileList) {

					imageFileName = imageDTO.getImageFileName();
					File srcFile = new File(ARTICLE_IMAGE_REPO1 + "//" + "temp" + "//" + imageFileName);
					File destFile = new File(ARTICLE_IMAGE_REPO1 + "//" + post_num);
					FileUtils.moveFileToDirectory(srcFile, destFile, true);
				}
			}

			message = "<script>";
			message += " alert('새글을 추가했습니다.');";
			message += " location.href='" + multipartRequest.getContextPath() + "/board/community-info';";
			message += "</script>";

			// 새 글을 추가한 후 메시지를 전달함
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);

		} catch (Exception e) {
			if (imageFileList != null && imageFileList.size() != 0) {
				// 오류 발생시 temp폴더의 이미지들 모두 삭제
				for (ImageDTO imageDTO : imageFileList) {
					imageFileName = imageDTO.getImageFileName();
					File srcFile = new File(ARTICLE_IMAGE_REPO1 + "//" + "temp" + "//" + imageFileName);
					srcFile.delete();
				}
			}

			message = "<script>";
			message += " alert('빈칸없이 입력해 주세요!.');";
			message += " location.href='" + multipartRequest.getContextPath() + "/board/community-info';";
			message += "</script>";
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);

			e.printStackTrace();
		}

		return resEnt;
	}

	// 이미지 업로드
	private List<String> upload(MultipartHttpServletRequest multipartRequest) throws ServletException, IOException {
		List<String> fileList = new ArrayList<>();
		Iterator<String> fileNames = multipartRequest.getFileNames();
		while (fileNames.hasNext()) {
			String fileName = fileNames.next();
			MultipartFile mFile = multipartRequest.getFile(fileName);
			String originalFilename = mFile.getOriginalFilename();

			if (originalFilename != "" && originalFilename != null) {
				fileList.add(originalFilename); // 첨부한 이미지 파일의 이름들을 차례대로 저장함
				File file = new File(ARTICLE_IMAGE_REPO + "//" + fileName);
				if (mFile.getSize() != 0) {
					if (!file.exists()) {
						file.getParentFile().mkdirs(); // 경로에 해당하는 디렉토리들 생성
						mFile.transferTo(new File(ARTICLE_IMAGE_REPO + "//" + "temp" + "//" + originalFilename)); // 임시로
						// 저장된 MultipartFile을 실제 파일로 전송
					}
				}
			}
		}

		return fileList;
	}

	// 이미지 업로드1
	private List<String> upload1(MultipartHttpServletRequest multipartRequest) throws ServletException, IOException {
		List<String> fileList = new ArrayList<>();
		Iterator<String> fileNames = multipartRequest.getFileNames();
		while (fileNames.hasNext()) {
			String fileName = fileNames.next();
			MultipartFile mFile = multipartRequest.getFile(fileName);
			String originalFilename = mFile.getOriginalFilename();

			if (originalFilename != "" && originalFilename != null) {
				fileList.add(originalFilename); // 첨부한 이미지 파일의 이름들을 차례대로 저장함
				File file = new File(ARTICLE_IMAGE_REPO1 + "//" + fileName);
				if (mFile.getSize() != 0) {
					if (!file.exists()) {
						file.getParentFile().mkdirs(); // 경로에 해당하는 디렉토리들 생성
						mFile.transferTo(new File(ARTICLE_IMAGE_REPO1 + "//" + "temp" + "//" + originalFilename)); // 임시로
						// 저장된 MultipartFile을 실제 파일로 전송
					}
				}
			}
		}

		return fileList;
	}

	// 페이징 검색
	@RequestMapping(value = "/board/community-acco", method = RequestMethod.GET)
	public void listPage(@ModelAttribute("scri") SearchCriteria scri, Model model) throws Exception {

		List<BoardDTO> boardsList = boardService.listfind(scri);
		model.addAttribute("boardsList", boardsList);

		PageMaker pageMaker = new PageMaker();
		pageMaker.setCri(scri);
		pageMaker.setTotalCount(boardService.findlistCount(scri));
		model.addAttribute("pageMaker", pageMaker);
	}

	// 페이징 검색1
	@RequestMapping(value = "/board/community-info", method = RequestMethod.GET)
	public void listPage1(@ModelAttribute("scri") SearchCriteria scri, Model model) throws Exception {

		List<BoardDTO> boardsList = boardService.listfind1(scri);
		model.addAttribute("boardsList", boardsList);

		PageMaker pageMaker = new PageMaker();
		pageMaker.setCri(scri);
		pageMaker.setTotalCount(boardService.findlistCount1(scri));
		model.addAttribute("pageMaker", pageMaker);
	}

	// 글 수정 페이지
	@Override
	@RequestMapping(value = "/board/modBoard.do", method = RequestMethod.GET)
	public ModelAndView update(@RequestParam(value = "post_num") int post_num, // 조회할 글 번호를 가져옴
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		Map<String, Object> boardMap = boardService.viewdetail(post_num); // 조회할 글 정보,이미지파일 정보를 articleMap에 설정

		ModelAndView mav = new ModelAndView();
		mav.setViewName("/board/modBoard.do");
		mav.addObject("boardMap", boardMap);

		return mav;
	}

	// 글 수정 페이지 1
	@Override
	@RequestMapping(value = "/board/modBoard1.do", method = RequestMethod.GET)
	public ModelAndView update1(@RequestParam(value = "post_num") int post_num, // 조회할 글 번호를 가져옴
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		Map<String, Object> boardMap = boardService.viewdetail1(post_num); // 조회할 글 정보,이미지파일 정보를 articleMap에 설정

		ModelAndView mav = new ModelAndView();
		mav.setViewName("/board/modBoard1.do");
		mav.addObject("boardMap", boardMap);

		return mav;
	}

	// 글 수정하기
	@Override
	@RequestMapping(value = "/board/modBoard.do", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity modBoard(MultipartHttpServletRequest multipartRequest, HttpServletResponse response)
			throws Exception {

		multipartRequest.setCharacterEncoding("utf-8");

		Map<String, Object> boardMap = new HashMap<>();

		Enumeration enu = multipartRequest.getParameterNames();
		while (enu.hasMoreElements()) {
			String name = (String) enu.nextElement();

			if (name.equals("imageFileNO")) {
				String[] values = multipartRequest.getParameterValues(name);
				boardMap.put(name, values);
			} else if (name.equals("oldFileName")) {
				String[] values = multipartRequest.getParameterValues(name);
				boardMap.put(name, values);
			} else {
				String value = multipartRequest.getParameter(name);
				boardMap.put(name, value);
			}
		}

		// 수정한 이미지 파일을 업로드함
		List<String> fileList = uploadModImageFile(multipartRequest);

		// 수정시 새로 추가된 이미지 수
		int added_img_num = Integer.parseInt((String) boardMap.get("added_img_num"));

		// 기존 이미지 수
		int pre_img_num = Integer.parseInt((String) boardMap.get("pre_img_num"));

		List<ImageDTO> imageFileList = new ArrayList<>();
		List<ImageDTO> modAddImageFileList = new ArrayList<>();

		if (fileList != null && fileList.size() != 0) {
			String[] imageFileNO = (String[]) boardMap.get("imageFileNO");

			for (int i = 0; i < added_img_num; i++) {
				String fileName = fileList.get(i);
				ImageDTO imageDTO = new ImageDTO();
				if (i < pre_img_num) { // 기존의 이미지를 수정해서 첨부한 이미지들
					imageDTO.setImageFileName(fileName);
					imageDTO.setImageFileNO(Integer.parseInt(imageFileNO[i]));
					imageFileList.add(imageDTO);
					boardMap.put("imageFileList", imageFileList);
				} else { // 새로 추가한 이미지들
					imageDTO.setImageFileName(fileName);
					modAddImageFileList.add(imageDTO); // ??
					boardMap.put("modAddImageFileList", modAddImageFileList);
				}
			}

		}

		String post_num = (String) boardMap.get("post_num");
		String message;
		ResponseEntity resEnt = null;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/html; charset=utf-8");

		try {
			boardService.modBoard(boardMap);

			if (fileList != null && fileList.size() != 0) {
				for (int i = 0; i < fileList.size(); i++) {
					String fileName = fileList.get(i);

					if (i < pre_img_num) {
						if (fileName != null) {
							File srcFile = new File(ARTICLE_IMAGE_REPO + "//" + "temp" + "//" + fileName);
							File destFile = new File(ARTICLE_IMAGE_REPO + "//" + post_num);
							FileUtils.moveFileToDirectory(srcFile, destFile, true);

							String[] oldName = (String[]) boardMap.get("oldFileName");
							String oldFileName = oldName[i];

							File oldFile = new File(ARTICLE_IMAGE_REPO + "//" + post_num + "//" + oldFileName);
							oldFile.delete();
						}
					} else {
						if (fileName != null) {
							File srcFile = new File(ARTICLE_IMAGE_REPO + "//" + "temp" + "//" + fileName);
							File destFile = new File(ARTICLE_IMAGE_REPO + "//" + post_num);
							FileUtils.moveFileToDirectory(srcFile, destFile, true);
						}
					}

				}
			}

			message = "<script>";
			message += " alert('글을 수정했습니다.');";
			message += " location.href='" + multipartRequest.getContextPath() + "/board/community_detail.do?post_num="
					+ post_num + "';";
			message += "</script>";

			// 새 글을 추가한 후 메시지를 전달함
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);

		} catch (Exception e) {
			if (fileList != null && fileList.size() != 0) {
				// 오류 발생시 temp폴더의 이미지들 모두 삭제
				for (int i = 0; i < fileList.size(); i++) {
					File srcFile = new File(ARTICLE_IMAGE_REPO + "//" + "temp" + "//" + fileList.get(i));
					srcFile.delete();
				}
			}

			message = "<script>";

			message += " location.href='" + multipartRequest.getContextPath() + "/board/community_detail.do?post_num="
					+ post_num + "';";
			message += "</script>";
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);

			e.printStackTrace();
		}

		return resEnt;
	}

	// 수정시 다중 이미지 업로드하기
	private List<String> uploadModImageFile(MultipartHttpServletRequest multipartRequest)
			throws Exception, IOException {

		List<String> fileList = new ArrayList<>();
		Iterator<String> fileNames = multipartRequest.getFileNames();

		while (fileNames.hasNext()) {
			String fileName = fileNames.next();

			MultipartFile mFile = multipartRequest.getFile(fileName);
			String originalFileName = mFile.getOriginalFilename();
			if (originalFileName != "" && originalFileName != null) {
				fileList.add(originalFileName);

				File file = new File(ARTICLE_IMAGE_REPO + "//" + fileName);
				if (mFile.getSize() != 0) {
					if (!file.exists()) {
						file.getParentFile().mkdirs(); // 경로에 해당하는 디렉토리들 생성
						mFile.transferTo(new File(ARTICLE_IMAGE_REPO + "//" + "temp" + "//" + originalFileName)); // 임시로
						// 저장된 MultipartFile을 실제 파일로 전송
					}
				}

			} else { // 첨부한 이미지가 없었을 경우
				fileList.add(null);
			}
		}

		return fileList;
	}

	// 수정시 다중 이미지 업로드하기
	private List<String> uploadModImageFile1(MultipartHttpServletRequest multipartRequest)
			throws Exception, IOException {

		List<String> fileList = new ArrayList<>();
		Iterator<String> fileNames = multipartRequest.getFileNames();

		while (fileNames.hasNext()) {
			String fileName = fileNames.next();

			MultipartFile mFile = multipartRequest.getFile(fileName);
			String originalFileName = mFile.getOriginalFilename();
			if (originalFileName != "" && originalFileName != null) {
				fileList.add(originalFileName);

				File file = new File(ARTICLE_IMAGE_REPO1 + "//" + fileName);
				if (mFile.getSize() != 0) {
					if (!file.exists()) {
						file.getParentFile().mkdirs(); // 경로에 해당하는 디렉토리들 생성
						mFile.transferTo(new File(ARTICLE_IMAGE_REPO1 + "//" + "temp" + "//" + originalFileName)); // 임시로
						// 저장된 MultipartFile을 실제 파일로 전송
					}
				}

			} else { // 첨부한 이미지가 없었을 경우
				fileList.add(null);
			}
		}

		return fileList;
	}

	// 글 수정하기
	@Override
	@RequestMapping(value = "/board/modBoard1.do", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity modBoard1(MultipartHttpServletRequest multipartRequest, HttpServletResponse response)
			throws Exception {

		multipartRequest.setCharacterEncoding("utf-8");

		Map<String, Object> boardMap = new HashMap<>();

		Enumeration enu = multipartRequest.getParameterNames();
		while (enu.hasMoreElements()) {
			String name = (String) enu.nextElement();

			if (name.equals("imageFileNO")) {
				String[] values = multipartRequest.getParameterValues(name);
				boardMap.put(name, values);
			} else if (name.equals("oldFileName")) {
				String[] values = multipartRequest.getParameterValues(name);
				boardMap.put(name, values);
			} else {
				String value = multipartRequest.getParameter(name);
				boardMap.put(name, value);
			}
		}

		// 수정한 이미지 파일을 업로드함
		List<String> fileList = uploadModImageFile1(multipartRequest);

		// 수정시 새로 추가된 이미지 수
		int added_img_num = Integer.parseInt((String) boardMap.get("added_img_num"));

		// 기존 이미지 수
		int pre_img_num = Integer.parseInt((String) boardMap.get("pre_img_num"));

		List<ImageDTO> imageFileList = new ArrayList<>();
		List<ImageDTO> modAddImageFileList = new ArrayList<>();

		if (fileList != null && fileList.size() != 0) {
			String[] imageFileNO = (String[]) boardMap.get("imageFileNO");

			for (int i = 0; i < added_img_num; i++) {
				String fileName = fileList.get(i);
				ImageDTO imageDTO = new ImageDTO();
				if (i < pre_img_num) { // 기존의 이미지를 수정해서 첨부한 이미지들
					imageDTO.setImageFileName(fileName);
					imageDTO.setImageFileNO(Integer.parseInt(imageFileNO[i]));
					imageFileList.add(imageDTO);
					boardMap.put("imageFileList", imageFileList);
				} else { // 새로 추가한 이미지들
					imageDTO.setImageFileName(fileName);
					modAddImageFileList.add(imageDTO); // ??
					boardMap.put("modAddImageFileList", modAddImageFileList);
				}
			}

		}

		String post_num = (String) boardMap.get("post_num");
		String message;
		ResponseEntity resEnt = null;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/html; charset=utf-8");

		try {
			boardService.modBoard1(boardMap);

			if (fileList != null && fileList.size() != 0) {
				for (int i = 0; i < fileList.size(); i++) {
					String fileName = fileList.get(i);

					if (i < pre_img_num) {
						if (fileName != null) {
							File srcFile = new File(ARTICLE_IMAGE_REPO1 + "//" + "temp" + "//" + fileName);
							File destFile = new File(ARTICLE_IMAGE_REPO1 + "//" + post_num);
							FileUtils.moveFileToDirectory(srcFile, destFile, true);

							String[] oldName = (String[]) boardMap.get("oldFileName");
							String oldFileName = oldName[i];

							File oldFile = new File(ARTICLE_IMAGE_REPO1 + "//" + post_num + "//" + oldFileName);
							oldFile.delete();
						}
					} else {
						if (fileName != null) {
							File srcFile = new File(ARTICLE_IMAGE_REPO1 + "//" + "temp" + "//" + fileName);
							File destFile = new File(ARTICLE_IMAGE_REPO1 + "//" + post_num);
							FileUtils.moveFileToDirectory(srcFile, destFile, true);
						}
					}

				}
			}

			message = "<script>";
			message += " alert('글을 수정했습니다.');";
			message += " location.href='" + multipartRequest.getContextPath() + "/board/community_detail2.do?post_num="
					+ post_num + "';";
			message += "</script>";

			// 새 글을 추가한 후 메시지를 전달함
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);

		} catch (Exception e) {
			if (fileList != null && fileList.size() != 0) {
				// 오류 발생시 temp폴더의 이미지들 모두 삭제
				for (int i = 0; i < fileList.size(); i++) {
					File srcFile = new File(ARTICLE_IMAGE_REPO1 + "//" + "temp" + "//" + fileList.get(i));
					srcFile.delete();
				}
			}

			message = "<script>";
			message += " alert('오류가 발생했습니다. 다시 시도해 주세요.');";
			message += " location.href='" + multipartRequest.getContextPath() + "/board/community_detail2.do?post_num="
					+ post_num + "';";
			message += "</script>";
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);

			e.printStackTrace();
		}

		return resEnt;
	}

	// 글삭제1
	@Override
	@RequestMapping(value = "/board/removeBoard.do", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity removeBoard(@RequestParam("post_num") int post_num, // 삭제할 글번호 가져옴
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		response.setContentType("text/html; charset=utf-8");

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/html; charset=utf-8");

		String message;
		ResponseEntity resEnt = null;

		try {
			boardService.removeBoard(post_num); // 글번호 전달해서 글 삭제함

			File destDir = new File(ARTICLE_IMAGE_REPO + "//" + post_num);
			FileUtils.deleteDirectory(destDir); // 첨부된 이미지 파일이 저장된 폴더도 삭제함

			message = "<script>";
			message += " alert('글을 삭제했습니다.');";
			message += " location.href='" + request.getContextPath() + "/board/community-acco.do';";
			message += "</script>";

			// 글 삭제 후 메시지를 전달함
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
		} catch (Exception e) {
			message = "<script>";
			message += " alert('글을 삭제하는 중 오류가 발생했습니다. 다시 시도해 주세요.');";
			message += " location.href='" + request.getContextPath() + "/board/community-acco.do';";
			message += "</script>";
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);

			e.printStackTrace();
		}

		return resEnt;
	}
	
	// 글삭제 마스터
	@Override
	@RequestMapping(value = "/board/removeBoard2.do", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity removeBoard2(@RequestParam("post_num") int post_num, // 삭제할 글번호 가져옴
			HttpServletRequest request, HttpServletResponse response) throws Exception {

			response.setContentType("text/html; charset=utf-8");

			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.add("Content-Type", "text/html; charset=utf-8");

			String message;
			ResponseEntity resEnt = null;

			try {
				boardService.removeBoard(post_num); // 글번호 전달해서 글 삭제함

				File destDir = new File(ARTICLE_IMAGE_REPO + "//" + post_num);
				FileUtils.deleteDirectory(destDir); // 첨부된 이미지 파일이 저장된 폴더도 삭제함

				message = "<script>";
				message += " alert('글을 삭제했습니다.');";
				message += " location.href='" + request.getContextPath() + "/board/community-accomaster.do';";
				message += "</script>";

				// 글 삭제 후 메시지를 전달함
				resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
			} catch (Exception e) {
				message = "<script>";
				message += " alert('글을 삭제하는 중 오류가 발생했습니다. 다시 시도해 주세요.');";
				message += " location.href='" + request.getContextPath() + "/board/community-accomaster.do';";
				message += "</script>";
				resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);

				e.printStackTrace();
			}

			return resEnt;
		}

	// 글삭제2
	@Override
	@RequestMapping(value = "/board/removeBoard1.do", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity removeBoard1(@RequestParam("post_num") int post_num, // 삭제할 글번호 가져옴
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		response.setContentType("text/html; charset=utf-8");

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/html; charset=utf-8");

		String message;
		ResponseEntity resEnt = null;

		try {
			boardService.removeBoard1(post_num); // 글번호 전달해서 글 삭제함

			File destDir = new File(ARTICLE_IMAGE_REPO1 + "//" + post_num);
			FileUtils.deleteDirectory(destDir); // 첨부된 이미지 파일이 저장된 폴더도 삭제함

			message = "<script>";
			message += " alert('글을 삭제했습니다.');";
			message += " location.href='" + request.getContextPath() + "/board/community-info.do';";
			message += "</script>";

			// 글 삭제 후 메시지를 전달함
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
		} catch (Exception e) {
			message = "<script>";
			message += " alert('글을 삭제하는 중 오류가 발생했습니다. 다시 시도해 주세요.');";
			message += " location.href='" + request.getContextPath() + "/board/community-info.do';";
			message += "</script>";
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);

			e.printStackTrace();
		}

		return resEnt;
	}
	
	// 글삭제2
	@Override
	@RequestMapping(value = "/board/removeBoard3.do", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity removeBoard3(@RequestParam("post_num") int post_num, // 삭제할 글번호 가져옴
			HttpServletRequest request, HttpServletResponse response) throws Exception {

			response.setContentType("text/html; charset=utf-8");

			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.add("Content-Type", "text/html; charset=utf-8");

			String message;
			ResponseEntity resEnt = null;

			try {
				boardService.removeBoard1(post_num); // 글번호 전달해서 글 삭제함

				File destDir = new File(ARTICLE_IMAGE_REPO1 + "//" + post_num);
				FileUtils.deleteDirectory(destDir); // 첨부된 이미지 파일이 저장된 폴더도 삭제함

				message = "<script>";
				message += " alert('글을 삭제했습니다.');";
				message += " location.href='" + request.getContextPath() + "/board/community-infomaster.do';";
				message += "</script>";

				// 글 삭제 후 메시지를 전달함
				resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
			} catch (Exception e) {
				message = "<script>";
				message += " alert('글을 삭제하는 중 오류가 발생했습니다. 다시 시도해 주세요.');";
				message += " location.href='" + request.getContextPath() + "/board/community-infomaster.do';";
				message += "</script>";
				resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);

				e.printStackTrace();
			}

			return resEnt;
		}

	// 글 이미지 삭제
	@Override
	@RequestMapping(value = "/board/removeMod.do", method = RequestMethod.POST)
	public void removeMod(HttpServletRequest request, HttpServletResponse response) throws Exception {
		request.setCharacterEncoding("utf-8");
		response.setContentType("text/html; charset=utf-8");
		PrintWriter writer = response.getWriter();

		String imageFileNO = request.getParameter("imageFileNO");
		String imageFileName = request.getParameter("imageFileName");
		String post_num = request.getParameter("post_num");

		System.out.println("imageFileNO= " + imageFileNO);
		System.out.println("post_num= " + post_num);

		ImageDTO imageDTO = new ImageDTO();
		imageDTO.setpost_num(Integer.parseInt(post_num));
		imageDTO.setImageFileNO(Integer.parseInt(imageFileNO));

		boardService.removeModImage(imageDTO);

		File oldFile = new File(ARTICLE_IMAGE_REPO + "//" + post_num + "//" + imageFileName);
		oldFile.delete();

		writer.print("success");

	}

	// 글 이미지 삭제1
	@Override
	@RequestMapping(value = "/board/removeMod1.do", method = RequestMethod.POST)
	public void removeMod1(HttpServletRequest request, HttpServletResponse response) throws Exception {
		request.setCharacterEncoding("utf-8");
		response.setContentType("text/html; charset=utf-8");
		PrintWriter writer = response.getWriter();

		String imageFileNO = request.getParameter("imageFileNO");
		String imageFileName = request.getParameter("imageFileName");
		String post_num = request.getParameter("post_num");

		System.out.println("imageFileNO= " + imageFileNO);
		System.out.println("post_num= " + post_num);

		ImageDTO imageDTO = new ImageDTO();
		imageDTO.setpost_num(Integer.parseInt(post_num));
		imageDTO.setImageFileNO(Integer.parseInt(imageFileNO));

		boardService.removeModImage1(imageDTO);

		File oldFile = new File(ARTICLE_IMAGE_REPO1 + "//" + post_num + "//" + imageFileName);
		oldFile.delete();

		writer.print("success");

	}

	// 추천
	@RequestMapping(value = "/board/updateLike", method = RequestMethod.POST)
	@ResponseBody
	public int updateLike(int post_num, String id) throws Exception {

		int likeCheck = boardService.likeCheck(post_num, id);

		if (likeCheck == 0) {
			// 좋아요 처음누름
			boardService.insertLike(post_num, id); // like테이블 삽입
			boardService.updateLike(post_num); // 게시판테이블 +1
			boardService.updateLikeCheck(post_num, id);// like테이블 구분자 1

		} else if (likeCheck == 1) {
			boardService.updateLikeCheckCancel(post_num, id); // like테이블 구분자0
			boardService.updateLikeCancel(post_num); // 게시판테이블 - 1
			boardService.deleteLike(post_num, id); // like테이블 삭제
		}
		return likeCheck;
	}

	// 추천1
	@RequestMapping(value = "/board/updateLike1", method = RequestMethod.POST)
	@ResponseBody
	public int updateLike1(int post_num, String id) throws Exception {

		int likeCheck = boardService.likeCheck1(post_num, id);

		if (likeCheck == 0) {
			boardService.insertLike1(post_num, id); // like테이블 삽입
			boardService.updateLike1(post_num); // 게시판테이블 +1
			boardService.updateLikeCheck1(post_num, id);// like테이블 구분자 1
		} else if (likeCheck == 1) {
			boardService.updateLikeCheckCancel1(post_num, id); // like테이블 구분자0
			boardService.updateLikeCancel1(post_num); // 게시판테이블 - 1
			boardService.deleteLike1(post_num, id); // like테이블 삭제
		}
		return likeCheck;
	}

	// 신고
	@RequestMapping(value = "/board/updatesin", method = RequestMethod.POST)
	@ResponseBody
	public int updatesin(int post_num, String id) throws Exception {

		int sinCheck = boardService.sinCheck(post_num, id);

		if (sinCheck == 0) {

			boardService.insertsin(post_num, id);
			boardService.updatesin(post_num);
			boardService.updatesinCheck(post_num, id);

		} else if (sinCheck == 1) {
			boardService.updatesinCheckCancel(post_num, id);
			boardService.updatesinCancel(post_num);
			boardService.deletesin(post_num, id);
		}
		return sinCheck;
	}

	// 신고2
	@RequestMapping(value = "/board/updatesin1", method = RequestMethod.POST)
	@ResponseBody
	public int updatesin1(int post_num, String id) throws Exception {

		int sinCheck = boardService.sinCheck1(post_num, id);

		if (sinCheck == 0) {

			boardService.insertsin1(post_num, id);
			boardService.updatesin1(post_num);
			boardService.updatesinCheck1(post_num, id);

		} else if (sinCheck == 1) {
			boardService.updatesinCheckCancel1(post_num, id);
			boardService.updatesinCancel1(post_num);
			boardService.deletesin1(post_num, id);
		}
		return sinCheck;
	}

	// 댓글 작성
	@PostMapping("board/boardreplyWrite")
	public String boardreplyWrite(boardCommentDTO boardCommentDTO, BoardDTO boardDTO, Criteria cri,
			RedirectAttributes rttr) throws Exception {
		log.info("reply write");
		boardService.boardregister(boardCommentDTO);
		rttr.addAttribute("post_num", boardCommentDTO.getPost_num());

		return "redirect:/board/community_detail.do";
	}

	// 댓글 수정 페이지
	@GetMapping("board/boardreplyUpdateView")
	public String boardreplyUpdateView(boardCommentDTO boardCommentDTO, Criteria cri, Model model) throws Exception {
		log.info("reply write");

		boardCommentDTO reply = boardService.boardselectReply(boardCommentDTO.getCom_num());
		log.info("댓글번호 : " + reply.getCom_num());
		model.addAttribute("replyUpdate", boardService.boardselectReply(boardCommentDTO.getCom_num()));
		model.addAttribute("cri", cri);

		return "board/boardreplyUpdateView";
	}

	// 댓글 수정 폼
	@PostMapping("board/boardreplyUpdate")
	public String boardreplyUpdate(boardCommentDTO boardCommentDTO, Criteria cri, RedirectAttributes rttr)
			throws Exception {
		log.info("reply Write");

		boardService.boardmodify(boardCommentDTO);

		rttr.addAttribute("post_num", boardCommentDTO.getPost_num());

		return "redirect:/board/community_detail.do";
	}

	// 댓글 삭제 폼
	@PostMapping("board/boardreplyDelete")
	public String boardreplyDelete(boardCommentDTO boardCommentDTO, BoardDTO boardDTO, Criteria cri, Model model,
			RedirectAttributes rttr) throws Exception {
		log.info("reply delete");

		boardService.boardremove(boardCommentDTO);
		rttr.addAttribute("post_num", boardCommentDTO.getPost_num());

		return "redirect:/board/community_detail.do";
	}

	// 댓글2
	// 댓글 작성
	@PostMapping("board/boardreplyWrite2")
	public String boardreplyWrite2(boardCommentDTO boardCommentDTO, BoardDTO boardDTO, Criteria cri,
			RedirectAttributes rttr) throws Exception {
		log.info("reply write2");
		boardService.boardregister2(boardCommentDTO);
		rttr.addAttribute("post_num", boardCommentDTO.getPost_num());

		return "redirect:/board/community_detail2.do";
	}

	// 댓글 수정 페이지
	@GetMapping("board/boardreplyUpdateView2")
	public String boardreplyUpdateView2(boardCommentDTO boardCommentDTO, Criteria cri, Model model) throws Exception {
		log.info("reply write2");

		boardCommentDTO reply = boardService.boardselectReply2(boardCommentDTO.getCom_num());
		log.info("댓글번호 : " + reply.getCom_num());
		model.addAttribute("replyUpdate", boardService.boardselectReply2(boardCommentDTO.getCom_num()));
		model.addAttribute("cri", cri);

		return "board/boardreplyUpdateView2";
	}

	// 댓글 수정 폼
	@PostMapping("board/boardreplyUpdate2")
	public String boardreplyUpdate2(boardCommentDTO boardCommentDTO, Criteria cri, RedirectAttributes rttr)
			throws Exception {
		log.info("reply Write2");

		boardService.boardmodify2(boardCommentDTO);

		rttr.addAttribute("post_num", boardCommentDTO.getPost_num());

		return "redirect:/board/community_detail2.do";
	}

	// 댓글 삭제 폼
	@PostMapping("board/boardreplyDelete2")
	public String boardreplyDelete2(boardCommentDTO boardCommentDTO, BoardDTO boardDTO, Criteria cri, Model model,
			RedirectAttributes rttr) throws Exception {
		log.info("reply delete");

		boardService.boardremove2(boardCommentDTO);
		rttr.addAttribute("post_num", boardCommentDTO.getPost_num());

		return "redirect:/board/community_detail2.do";
	}

	// 페이징 검색
	@RequestMapping(value = "/board/community-accomaster", method = RequestMethod.GET)
	public void listPagemaster(@ModelAttribute("scri") SearchCriteria scri, Model model) throws Exception {

		List<BoardDTO> boardsList = boardService.listfindmaster(scri);
		model.addAttribute("boardsList", boardsList);

		PageMaker pageMaker = new PageMaker();
		pageMaker.setCri(scri);
		pageMaker.setTotalCount(boardService.findlistCountmaster(scri));
		model.addAttribute("pageMaker", pageMaker);
	}

	// 페이징 검색 관리자 신고수 체크
	@RequestMapping(value = "/board/community-infomaster", method = RequestMethod.GET)
	public void listPage1master(@ModelAttribute("scri") SearchCriteria scri, Model model) throws Exception {

		List<BoardDTO> boardsList = boardService.listfindmaster1(scri);
		model.addAttribute("boardsList", boardsList);

		PageMaker pageMaker = new PageMaker();
		pageMaker.setCri(scri);
		pageMaker.setTotalCount(boardService.findlistCountmaster1(scri));
		model.addAttribute("pageMaker", pageMaker);
	}	
}